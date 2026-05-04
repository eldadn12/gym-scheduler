package algorithm;

import model.*;
import enums.MuscleGroup;

import java.util.*;

/**
 * מחלקת הליבה של המערכת, המהווה את מנוע השיבוץ החכם.
 * גישה היברידית: Backtracking עם היוריסטיקות למציאת פתרון ראשוני איכותי,
 * ולאחריו שלב Optimization מבוסס תורת הגרפים לשיפור סופי של איזון העומסים.
 */
public class ScheduleSolver {

    private static final int START_HOUR = 8;
    private static final int END_HOUR = 20;
    private static final int HOURS_COUNT = END_HOUR - START_HOUR + 1; // 13
    // --- מחלקות עזר פנימיות ---

    private static class TrainingRequest {
        Trainee trainee;
        MuscleGroup targetMuscle;

        public TrainingRequest(Trainee trainee, MuscleGroup targetMuscle) {
            this.trainee = trainee;
            this.targetMuscle = targetMuscle;
        }
    }

    private static class CandidateSlot {
        int day;
        int hour;
        Trainer trainer;
        Workout workout;
        int score;

        public CandidateSlot(int day, int hour, Trainer trainer, Workout workout, int score) {
            this.day = day;
            this.hour = hour;
            this.trainer = trainer;
            this.workout = workout;
            this.score = score;
        }
    }

    // --- מחלקות עזר לשלב האופטימיזציה (מידול כגרף פיזי) ---

    private static class AssignmentNode {
        int day, hour;
        Trainer trainer;

        AssignmentNode(int d, int h, Trainer t) {
            this.day = d;
            this.hour = h;
            this.trainer = t;
        }
    }

    private static class OptimizationEdge implements Comparable<OptimizationEdge> {
        AssignmentNode targetNode;
        int weight; // משקל הקשת (הציון)

        OptimizationEdge(AssignmentNode target, int weight) {
            this.targetNode = target;
            this.weight = weight;
        }

        @Override
        public int compareTo(OptimizationEdge other) {
            // מיון מהמשקל הגבוה לנמוך עבור תור העדיפויות
            return Integer.compare(other.weight, this.weight);
        }
    }

    // --- משתני המערכת ---
    private List<Trainer> trainers;
    private List<Workout> availableWorkouts;
    private List<Assignment> finalSchedule;

    private int[] dailyWorkoutCount;
    private Map<Trainer, Integer> trainerWorkload;
    private Map<Integer, boolean[][]> traineeHourlySchedule;

    private int maxWorkoutsPerDay;

    public ScheduleSolver(List<Trainer> trainers, List<Workout> availableWorkouts) {
        this.trainers = trainers;
        this.availableWorkouts = availableWorkouts;
        this.finalSchedule = new ArrayList<>();
        this.trainerWorkload = new HashMap<>();
        this.traineeHourlySchedule = new HashMap<>();
        this.dailyWorkoutCount = new int[7];

        for (Trainer t : trainers) {
            trainerWorkload.put(t, 0);
        }
    }

    /**
     * הפונקציה הראשית.
     */
    public List<Assignment> solve(List<Trainee> trainees) {
        for (Trainee t : trainees) {
            traineeHourlySchedule.put(t.getId(), new boolean[7][HOURS_COUNT]);
        }

        List<TrainingRequest> allRequests = new ArrayList<>();
        for (Trainee t : trainees) {
            for (MuscleGroup muscle : t.getRequiredWorkouts()) {
                allRequests.add(new TrainingRequest(t, muscle));
            }
        }

        this.maxWorkoutsPerDay = (allRequests.size() / 7) + 2;
        sortRequestsByDifficulty(allRequests);

        // הפעלת אלגוריתם ה-Backtracking המקורי שלך
        if (backtrack(allRequests, 0)) {
            // אם נמצא פתרון, מפעילים את שכבת האופטימיזציה כדי לשפר אותו!
            optimizeSchedule();
            return finalSchedule;
        } else {
            return new ArrayList<>(); // לא נמצא פתרון
        }
    }

    /**
     * פונקציית הנסיגה לאחור המקורית שלך - מתחשבת גם באילוצים קשיחים וגם ברכים.
     */
    private boolean backtrack(List<TrainingRequest> requests, int index) {
        if (index == requests.size()) {
            return true;
        }

        TrainingRequest currentReq = requests.get(index);
        Workout workoutType = findMatchingWorkout(currentReq.targetMuscle);
        if (workoutType == null) return false;

        List<CandidateSlot> candidates = new ArrayList<>();

        List<Integer> daysByLoad = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
        daysByLoad.sort((d1, d2) -> Integer.compare(dailyWorkoutCount[d1], dailyWorkoutCount[d2]));

        List<int[]> allTimeSlots = new ArrayList<>();
        for (int day : daysByLoad) {
            for (int hour = START_HOUR; hour <= END_HOUR; hour++) {
                allTimeSlots.add(new int[]{day, hour});
            }
        }

        for (int[] timeSlot : allTimeSlots) {
            int day = timeSlot[0];
            int hour = timeSlot[1];

            if (dailyWorkoutCount[day] < maxWorkoutsPerDay &&
                    currentReq.trainee.canTrainMuscle(currentReq.targetMuscle, day) &&
                    !isTraineeBusyThatDay(currentReq.trainee, day) &&
                    !traineeHourlySchedule.get(currentReq.trainee.getId())[day][hour-START_HOUR]) {

                for (Trainer trainer : trainers) {
                    if (isValidTrainerAssignment(trainer, currentReq.targetMuscle, day, hour)) {
                        int score = calculateSlotScore(trainer, day);
                        candidates.add(new CandidateSlot(day, hour, trainer, workoutType, score));
                    }
                }
            }
        }

        // המיון החמדני המקורי
        candidates.sort((c1, c2) -> Integer.compare(c2.score, c1.score));

        for (CandidateSlot slot : candidates) {
            if (!isFuturePruned(requests, index + 1, slot)) {

                // --- שלב הביצוע (Do) ---
                slot.trainer.bookSlot(slot.day, slot.hour);
                currentReq.trainee.updateMuscleTraining(currentReq.targetMuscle, slot.day);
                traineeHourlySchedule.get(currentReq.trainee.getId())[slot.day][slot.hour-START_HOUR] = true;
                trainerWorkload.put(slot.trainer, trainerWorkload.get(slot.trainer) + 1);
                dailyWorkoutCount[slot.day]++;

                Assignment newAssignment = new Assignment(currentReq.trainee, slot.trainer, slot.workout, slot.day, slot.hour);
                finalSchedule.add(newAssignment);

                if (backtrack(requests, index + 1)) return true;

                // --- שלב הביטול (Undo) ---
                finalSchedule.remove(finalSchedule.size() - 1);
                dailyWorkoutCount[slot.day]--;
                trainerWorkload.put(slot.trainer, trainerWorkload.get(slot.trainer) - 1);
                traineeHourlySchedule.get(currentReq.trainee.getId())[slot.day][slot.hour-START_HOUR] = false;
                currentReq.trainee.undoMuscleTraining(currentReq.targetMuscle);
                slot.trainer.freeSlot(slot.day, slot.hour);
            }
        }

        return false;
    }

    // ==========================================
    // שכבת האופטימיזציה החדשה (Hill Climbing with Graph)
    // ==========================================

    /**
     * פונקציה שעוברת על הלו"ז המלא שנמצא ומנסה לשפר את הציונים של השיבוצים
     * באמצעות מידול לגרף וחיפוש המסלול בעל המשקל המקסימלי (Best-First Search).
     */
    private void optimizeSchedule() {
        boolean improved = true;
        int maxIterations = 5;
        int iteration = 0;
        int MAX_TRAINER_HOURS = 35;

        while (improved && iteration < maxIterations) {
            improved = false;
            iteration++;

            for (int i = 0; i < finalSchedule.size(); i++) {
                Assignment currentAssignment = finalSchedule.get(i);
                MuscleGroup targetMuscle = getMuscleFromWorkout(currentAssignment.getWorkout());

                int currentScore = calculateSlotScore(currentAssignment.getTrainer(), currentAssignment.getDay());

                // מסירים זמנית את האימון כדי לבדוק חלופות
                removeAssignmentState(currentAssignment, targetMuscle);

                // יצירת תור עדיפויות לניהול הגרף (קשתות בעלות משקל)
                PriorityQueue<OptimizationEdge> possibleMovesGraph = new PriorityQueue<>();

                // בניית הגרף הפיזי של האפשרויות
                for (int day = 0; day < 7; day++) {
                    for (int hour = START_HOUR; hour <= END_HOUR; hour++) {

                        if (dailyWorkoutCount[day] < maxWorkoutsPerDay &&
                                currentAssignment.getTrainee().canTrainMuscle(targetMuscle, day) &&
                                !isTraineeBusyThatDay(currentAssignment.getTrainee(), day) &&
                                !traineeHourlySchedule.get(currentAssignment.getTrainee().getId())[day][hour-START_HOUR]) {

                            for (Trainer trainer : trainers) {
                                if (isValidTrainerAssignment(trainer, targetMuscle, day, hour) &&
                                        trainerWorkload.get(trainer) < MAX_TRAINER_HOURS) {

                                    int newScore = calculateSlotScore(trainer, day);

                                    // הוספת צומת וקשת לגרף (כל שיבוץ חוקי הוא שכן בגרף)
                                    AssignmentNode possibleNode = new AssignmentNode(day, hour, trainer);
                                    possibleMovesGraph.add(new OptimizationEdge(possibleNode, newScore));
                                }
                            }
                        }
                    }
                }

                // אלגוריתם על הגרף: שליפת הקשת הטובה ביותר
                OptimizationEdge bestMove = possibleMovesGraph.poll();

                // אם מצאנו שיבוץ בגרף שהוא טוב יותר מהנוכחי, נשמור אותו
                if (bestMove != null && bestMove.weight > currentScore) {
                    Assignment improvedAssignment = new Assignment(
                            currentAssignment.getTrainee(),
                            bestMove.targetNode.trainer,
                            currentAssignment.getWorkout(),
                            bestMove.targetNode.day,
                            bestMove.targetNode.hour
                    );
                    finalSchedule.set(i, improvedAssignment);
                    applyAssignmentState(improvedAssignment, targetMuscle);
                    improved = true;
                } else {
                    // לא נמצא שיפור, מחזירים את המצב המקורי
                    finalSchedule.set(i, currentAssignment);
                    applyAssignmentState(currentAssignment, targetMuscle);
                }
            }
        }
    }

    // פונקציות עזר לשלב האופטימיזציה שישמרו על הקוד נקי מבאגים
    private void applyAssignmentState(Assignment a, MuscleGroup muscle) {
        a.getTrainer().bookSlot(a.getDay(), a.getHour());
        a.getTrainee().updateMuscleTraining(muscle, a.getDay());
        traineeHourlySchedule.get(a.getTrainee().getId())[a.getDay()][a.getHour() - START_HOUR] = true;
        trainerWorkload.put(a.getTrainer(), trainerWorkload.get(a.getTrainer()) + 1);
        dailyWorkoutCount[a.getDay()]++;
    }

    private void removeAssignmentState(Assignment a, MuscleGroup muscle) {
        a.getTrainer().freeSlot(a.getDay(), a.getHour());
        a.getTrainee().undoMuscleTraining(muscle);
        traineeHourlySchedule.get(a.getTrainee().getId())[a.getDay()][a.getHour() - START_HOUR] = false;
        trainerWorkload.put(a.getTrainer(), trainerWorkload.get(a.getTrainer()) - 1);
        dailyWorkoutCount[a.getDay()]--;
    }

    private MuscleGroup getMuscleFromWorkout(Workout w) {
        return w.getMuscleGroups().isEmpty() ? null : w.getMuscleGroups().iterator().next();
    }

    private boolean isTraineeBusyThatDay(Trainee trainee, int day) {
        boolean[][] schedule = traineeHourlySchedule.get(trainee.getId());
        for (int h = 0; h < HOURS_COUNT; h++) {
            if (schedule[day][h]) return true;
        }
        return false;
    }

    private void sortRequestsByDifficulty(List<TrainingRequest> requests) {
        requests.sort((req1, req2) -> {
            int req1Load = req1.trainee.getRequiredWorkouts().size();
            int req2Load = req2.trainee.getRequiredWorkouts().size();
            return Integer.compare(req2Load, req1Load);
        });
    }

    private boolean isValidTrainerAssignment(Trainer trainer, MuscleGroup targetMuscle, int day, int hour) {
        if (!trainer.isAvailable(day, hour)) return false;
        return trainer.getSpecialties().contains(targetMuscle);
    }

    private int calculateSlotScore(Trainer trainer, int day) {
        int score = 1000;
        score -= (trainerWorkload.get(trainer) * 5);
        int workoutsToday = 0;
        for (int h = START_HOUR; h <= END_HOUR; h++) {
            if (!trainer.isAvailable(day, h)) workoutsToday++;
        }
        score += (workoutsToday * 15);
        score -= (dailyWorkoutCount[day] * 25);
        return score;
    }

    private boolean isFuturePruned(List<TrainingRequest> requests, int nextIndex, CandidateSlot prospectiveSlot) {
        int MAX_TRAINER_HOURS = 35;
        if (trainerWorkload.get(prospectiveSlot.trainer) + 1 >= MAX_TRAINER_HOURS) {
            return true;
        }
        if (nextIndex < requests.size()) {
            TrainingRequest nextReq = requests.get(nextIndex);
            return !hasValidFutureSlot(nextReq, prospectiveSlot, MAX_TRAINER_HOURS);
        }
        return false;
    }

    private boolean hasValidFutureSlot(TrainingRequest nextReq, CandidateSlot prospectiveSlot, int maxHours) {
        for (int day = 0; day < 7; day++) {
            int currentDayLoad = dailyWorkoutCount[day] + (day == prospectiveSlot.day ? 1 : 0);
            if (currentDayLoad < maxWorkoutsPerDay && nextReq.trainee.canTrainMuscle(nextReq.targetMuscle, day)) {
                for (int hour = START_HOUR; hour <= END_HOUR; hour++) {
                    if (!(day == prospectiveSlot.day && hour == prospectiveSlot.hour) &&
                            !traineeHourlySchedule.get(nextReq.trainee.getId())[day][hour - START_HOUR]) {
                        for (Trainer t : trainers) {
                            int workload = trainerWorkload.get(t) + (t.equals(prospectiveSlot.trainer) ? 1 : 0);
                            if (workload < maxHours && isValidTrainerAssignment(t, nextReq.targetMuscle, day, hour)) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Workout findMatchingWorkout(MuscleGroup muscle) {
        for (Workout w : availableWorkouts) {
            if (w.getMuscleGroups().contains(muscle)) return w;
        }
        return null;
    }
}