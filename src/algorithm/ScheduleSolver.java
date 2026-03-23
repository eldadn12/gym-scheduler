package algorithm;

import model.*;
import enums.MuscleGroup;

import java.util.*;

/**
 * מחלקת הליבה של המערכת, המהווה את מנוע השיבוץ החכם.
 * המחלקה פותרת את בעיית סיפוק האילוצים (CSP) באמצעות אלגוריתם נסיגה לאחור (Backtracking),
 * בשילוב עם היוריסטיקות חמדניות (Greedy) וגיזום ענפים (Forward Checking / Pruning).
 */
public class ScheduleSolver {

    // --- מחלקות עזר פנימיות ---

    // מייצג בקשה בודדת לאימון של מתאמן על קבוצת שריר מסוימת
    /**
     * מחלקת עזר פנימית המאגדת מתאמן וקבוצת שריר ספציפית שהוא נדרש לאמן.
     * משמשת לייצוג יחידת עבודה (Request) שהאלגוריתם מנסה לשבץ.
     */
    private static class TrainingRequest {
        Trainee trainee;
        MuscleGroup targetMuscle;

        /**
         * בנאי ליצירת בקשת אימון.
         * @param trainee המתאמן הדורש את האימון.
         * @param targetMuscle קבוצת השריר המיועדת לאימון זה.
         */
        public TrainingRequest(Trainee trainee, MuscleGroup targetMuscle) {
            this.trainee = trainee;
            this.targetMuscle = targetMuscle;
        }
    }

    // מייצג "משבצת" פוטנציאלית לשיבוץ, כולל הציון שחושב עבורה לשלב הבחירה החמדנית (Greedy)
    /**
     * מחלקת עזר פנימית המייצגת משבצת זמן אופציונלית לשיבוץ (מועמד).
     * כוללת את ציון האיכות של המשבצת המשמש לבחירה החמדנית.
     */
    private static class CandidateSlot {
        int day;
        int hour;
        Trainer trainer;
        Workout workout;
        int score;

        /**
         * בנאי ליצירת משבצת מועמדת לשיבוץ.
         * @param day היום בשבוע (0-6).
         * @param hour השעה ביום (8-20).
         * @param trainer המאמן המועמד להעביר את האימון.
         * @param workout סוג האימון המועמד.
         * @param score ציון האיכות שחושב למשבצת זו.
         */
        public CandidateSlot(int day, int hour, Trainer trainer, Workout workout, int score) {
            this.day = day;
            this.hour = hour;
            this.trainer = trainer;
            this.workout = workout;
            this.score = score;
        }
    }

    // --- משתני המערכת ---
    private List<Trainer> trainers;
    private List<Workout> availableWorkouts;
    private List<Assignment> finalSchedule;

    // משתני מעקב לניהול המצב באלגוריתם ה-Backtracking
    private int[] dailyWorkoutCount; // מעקב אחר כמות האימונים הכללית בכל יום (לאיזון העומס)
    private Map<Trainer, Integer> trainerWorkload; // מעקב אחר עומס המאמנים למניעת שחיקה
    private Map<Integer, boolean[][]> traineeHourlySchedule; // לו"ז מתאמנים למניעת כפל שיבוץ

    // תקרת מקסימום דינמית כדי להכריח את האלגוריתם לפזר אימונים על פני כל השבוע
    private int maxWorkoutsPerDay;

    /**
     * בנאי לאתחול פותר הבעיות עם נתוני הבסיס של חדר הכושר.
     * @param trainers רשימת כל המאמנים הזמינים במערכת.
     * @param availableWorkouts רשימת כל סוגי האימונים האפשריים (נשלף מה-DB).
     */
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
     * הפונקציה הראשית שמפעילה את פותר הבעיות (CSP - Constraint Satisfaction Problem).
     * הפונקציה מאתחלת את מרחב הבעיה, מגדירה אילוצים דינמיים ומפעילה את החיפוש.
     * @param trainees רשימת המתאמנים שיש לשבץ להם אימונים במהלך השבוע.
     * @return רשימה מלאה של שיבוצים (Assignments) המרכיבה את הלו"ז הסופי, או רשימה ריקה אם אין פתרון.
     */
    public List<Assignment> solve(List<Trainee> trainees) {
        // אתחול יומני המתאמנים
        for (Trainee t : trainees) {
            traineeHourlySchedule.put(t.getId(), new boolean[7][24]);
        }

        // הגדרת מרחב הבעיה: הפיכת דרישות המתאמנים לרשימת בקשות
        List<TrainingRequest> allRequests = new ArrayList<>();
        for (Trainee t : trainees) {
            for (MuscleGroup muscle : t.getRequiredWorkouts()) {
                allRequests.add(new TrainingRequest(t, muscle));
            }
        }

        // אילוץ קשיח דינמי (Dynamic Hard Constraint):
        // חישוב מקסימום אימונים מותר ליום, כדי למנוע דחיסה של כל האימונים לימים הראשונים בשבוע.
        this.maxWorkoutsPerDay = (allRequests.size() / 7) + 2;

        // שלב היוריסטי (Degree Heuristic): מיון הבקשות מהקשה לקל (מי שצריך הכי הרבה אימונים ישובץ ראשון)
        sortRequestsByDifficulty(allRequests);

        // הפעלת אלגוריתם ה-Backtracking
        if (backtrack(allRequests, 0)) {
            return finalSchedule; // נמצא פתרון חוקי
        } else {
            return new ArrayList<>(); // לא נמצא פתרון שעונה על כל האילוצים
        }
    }

    /**
     * פונקציית הנסיגה לאחור (Recursive Backtracking) בשילוב בחירה חמדנית (Greedy).
     * @param requests רשימת כל בקשות האימון שיש לשבץ.
     * @param index האינדקס של הבקשה הנוכחית שמנסים לשבץ כעת.
     * @return true אם המסלול הנוכחי הוביל לשיבוץ מלא ומוצלח, false אם הגענו למבוי סתום.
     */
    private boolean backtrack(List<TrainingRequest> requests, int index) {
        // תנאי עצירה: כל הבקשות שובצו בהצלחה
        if (index == requests.size()) {
            return true;
        }

        TrainingRequest currentReq = requests.get(index);
        Workout workoutType = findMatchingWorkout(currentReq.targetMuscle);
        if (workoutType == null) return false;

        List<CandidateSlot> candidates = new ArrayList<>();

        // גישת פיזור עומסים שוויוני
        // שלב 1: יוצרים רשימת ימים וממיינים אותה כך שהיום ה"ריק" ביותר כרגע ייבדק ראשון
        List<Integer> daysByLoad = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
        daysByLoad.sort((d1, d2) -> Integer.compare(dailyWorkoutCount[d1], dailyWorkoutCount[d2]));

        // שלב 2: יצירת רשימת המשבצות לפי סדר הימים הריק ביותר קודם
        List<int[]> allTimeSlots = new ArrayList<>();
        for (int day : daysByLoad) {
            for (int hour = 8; hour <= 20; hour++) {
                allTimeSlots.add(new int[]{day, hour});
            }
        }

        for (int[] timeSlot : allTimeSlots) {
            int day = timeSlot[0];
            int hour = timeSlot[1];

            // בדיקת כל האילוצים הקשיחים בבת אחת
            if (dailyWorkoutCount[day] < maxWorkoutsPerDay &&
                    currentReq.trainee.canTrainMuscle(currentReq.targetMuscle, day) &&
                    !isTraineeBusyThatDay(currentReq.trainee, day) &&
                    !traineeHourlySchedule.get(currentReq.trainee.getId())[day][hour]) {

                // בדיקת מאמנים זמינים עבור המשבצת הספציפית הזו ומתן ניקוד
                for (Trainer trainer : trainers) {
                    if (isValidTrainerAssignment(trainer, currentReq.targetMuscle, day, hour)) {
                        int score = calculateSlotScore(trainer, day);
                        candidates.add(new CandidateSlot(day, hour, trainer, workoutType, score));
                    }
                }
            }
        }

        // גישה חמדנית (Greedy Choice): מיון המועמדים לפי הציון מהגבוה לנמוך
        candidates.sort((c1, c2) -> Integer.compare(c2.score, c1.score));

        // מעבר על המועמדים הטובים ביותר וניסיון שיבוץ
        for (CandidateSlot slot : candidates) {
            // גיזום (Forward Checking): חיתוך ענפים מראש שיובילו בוודאות לכישלון בהמשך
            if (!isFuturePruned(requests, index + 1, slot)) {

                // --- שלב הביצוע (Do - Apply Assignment) ---
                slot.trainer.bookSlot(slot.day, slot.hour);
                currentReq.trainee.updateMuscleTraining(currentReq.targetMuscle, slot.day);
                traineeHourlySchedule.get(currentReq.trainee.getId())[slot.day][slot.hour] = true;
                trainerWorkload.put(slot.trainer, trainerWorkload.get(slot.trainer) + 1);
                dailyWorkoutCount[slot.day]++; // עדכון עומס יומי כללי של המכון

                Assignment newAssignment = new Assignment(currentReq.trainee, slot.trainer, slot.workout, slot.day, slot.hour);
                finalSchedule.add(newAssignment);

                // קריאה רקורסיבית לעומק העץ
                if (backtrack(requests, index + 1)) return true;

                // --- שלב הביטול (Undo - Backtrack) למקרה של מבוי סתום ---
                finalSchedule.remove(finalSchedule.size() - 1);
                dailyWorkoutCount[slot.day]--; // שחזור העומס היומי
                trainerWorkload.put(slot.trainer, trainerWorkload.get(slot.trainer) - 1);
                traineeHourlySchedule.get(currentReq.trainee.getId())[slot.day][slot.hour] = false;
                currentReq.trainee.undoMuscleTraining(currentReq.targetMuscle);
                slot.trainer.freeSlot(slot.day, slot.hour);
            }
        }

        return false; // מחזיר שקר כדי לגרום לאלגוריתם לחזור צעד אחורה (Backtrack)
    }

    /**
     * פונקציית עזר לבדיקה אם המתאמן כבר משובץ לאימון כלשהו במהלך אותו יום.
     * @param trainee המתאמן הנבדק.
     * @param day היום בשבוע.
     * @return true אם למתאמן כבר יש אימון ביום זה, אחרת false.
     */
    // פונקציית עזר לבדיקה אם המתאמן כבר מתאמן ביום מסוים (מחליפה את ה-break הפנימי)
    private boolean isTraineeBusyThatDay(Trainee trainee, int day) {
        boolean busy = false;
        boolean[][] schedule = traineeHourlySchedule.get(trainee.getId());
        for (int h = 8; h <= 20; h++) {
            if (schedule[day][h]) {
                busy = true;
            }
        }
        return busy;
    }

    // ==========================================
    // לוגיקת האילוצים וההיוריסטיקות
    // ==========================================

    /**
     * היוריסטיקה של דרגת קושי (Degree Heuristic):
     * מתעדף מתאמנים שיש להם יותר אימונים לבצע, כיוון שקשה יותר לשבץ אותם בהמשך.
     * @param requests רשימת הבקשות שיש למיין.
     */
    private void sortRequestsByDifficulty(List<TrainingRequest> requests) {
        requests.sort((req1, req2) -> {
            int req1Load = req1.trainee.getRequiredWorkouts().size();
            int req2Load = req2.trainee.getRequiredWorkouts().size();
            return Integer.compare(req2Load, req1Load);
        });
    }

    /**
     * אילוצים קשיחים (Hard Constraints) ברמת המאמן: זמינות והתמחות.
     * @param trainer המאמן הנבדק.
     * @param targetMuscle קבוצת השריר שהאימון דורש.
     * @param day יום השיבוץ.
     * @param hour שעת השיבוץ.
     * @return true אם המאמן כשיר, מתמחה בשריר המבוקש ופנוי בשעה זו.
     */
    private boolean isValidTrainerAssignment(Trainer trainer, MuscleGroup targetMuscle, int day, int hour) {
        if (!trainer.isAvailable(day, hour)) return false;
        return trainer.getSpecialties().contains(targetMuscle);
    }

    /**
     * אילוצים רכים (Soft Constraints) / פונקציית משקל:
     * מחשב ציון איכות לכל משבצת אפשרית כדי למקסם את איכות לוח הזמנים (איזון עומסים וריכוז משמרות).
     * @param trainer המאמן המועמד לשיבוץ.
     * @param day יום השיבוץ.
     * @return הציון המספרי של המשבצת (ככל שגבוה יותר כך עדיף).
     */
    private int calculateSlotScore(Trainer trainer, int day) {
        int score = 1000;

        // אילוץ רך 1: איזון עומסים בין המאמנים למניעת שחיקה (קנס על עומס קיים)
        score -= (trainerWorkload.get(trainer) * 5);

        // אילוץ רך 2: ריכוז משמרות (Clustering) - מתן בונוס על ריכוז אימונים למאמן באותו יום
        int workoutsToday = 0;
        for (int h = 8; h <= 20; h++) {
            if (!trainer.isAvailable(day, h)) {
                workoutsToday++;
            }
        }
        score += (workoutsToday * 15);

        // אילוץ רך 3: מניעת עומס כללי במכון (קנס משמעותי לימים שכבר עמוסים באימונים)
        score -= (dailyWorkoutCount[day] * 25);

        return score;
    }

    /**
     * בדיקה מוקדמת (Forward Checking / Pruning):
     * זיהוי מראש של צעדים שיגרמו בוודאות להפרת אילוצים בהמשך העץ ומאפשר חיתוך ענפים.
     * @param requests רשימת הבקשות המלאה.
     * @param nextIndex האינדקס של הבקשה הבאה בתור שעתידה להיות משובצת.
     * @param prospectiveSlot המשבצת הנוכחית ששוקלים לשבץ כעת.
     * @return true אם יש לגזום (לפסול) את המשבצת הנוכחית, false אם בטוח להמשיך לתוכה.
     */
    private boolean isFuturePruned(List<TrainingRequest> requests, int nextIndex, CandidateSlot prospectiveSlot) {
        int MAX_TRAINER_HOURS = 35; // מקסימום שעות עבודה שבועיות חוקיות למאמן

        // 1. בדיקת עומס עבודה למאמן הנוכחי (שלא נחרוג מהמקסימום)
        if (trainerWorkload.get(prospectiveSlot.trainer) + 1 >= MAX_TRAINER_HOURS) {
            return true; // גיזום הענף
        }

        // 2. הצצה קדימה (Forward Checking) - האם לבקשה הבאה נשארו משבצות?
        if (nextIndex < requests.size()) {
            TrainingRequest nextReq = requests.get(nextIndex);

            // במקום searchLoop עם break, נשתמש בפונקציה בוליאנית שמחזירה אם נמצאה משבצת
            return !hasValidFutureSlot(nextReq, prospectiveSlot, MAX_TRAINER_HOURS);
        }

        return false; // המסלול בטוח, אפשר להמשיך
    }

    /**
     * פונקציית עזר המבצעת את ההצצה קדימה (מחליפה את ה-searchLoop וה-break).
     * בודקת האם קיימת לפחות משבצת חוקית אחת עתידית עבור הבקשה הבאה.
     * @param nextReq הבקשה הבאה בתור לבדיקה.
     * @param prospectiveSlot המשבצת שכרגע נבדקת (כדי לא להתנגש איתה).
     * @param maxHours תקרת שעות המקסימום למאמן.
     * @return true אם נמצאה משבצת חוקית פוטנציאלית, false אם אין שום אפשרות.
     */
    // פונקציית עזר שמחליפה את ה-searchLoop וה-break כדי למצוא אם יש אופציה עתידית
    private boolean hasValidFutureSlot(TrainingRequest nextReq, CandidateSlot prospectiveSlot, int maxHours) {
        boolean found = false;
        for (int day = 0; day < 7; day++) {
            int currentDayLoad = dailyWorkoutCount[day] + (day == prospectiveSlot.day ? 1 : 0);

            if (currentDayLoad < maxWorkoutsPerDay && nextReq.trainee.canTrainMuscle(nextReq.targetMuscle, day)) {
                for (int hour = 8; hour <= 20; hour++) {
                    if (!(day == prospectiveSlot.day && hour == prospectiveSlot.hour) &&
                            !traineeHourlySchedule.get(nextReq.trainee.getId())[day][hour]) {

                        for (Trainer t : trainers) {
                            int estimatedWorkload = trainerWorkload.get(t) + (t.getId() == prospectiveSlot.trainer.getId() ? 1 : 0);
                            if (estimatedWorkload < maxHours && isValidTrainerAssignment(t, nextReq.targetMuscle, day, hour)) {
                                found = true;
                            }
                        }
                    }
                }
            }
        }
        return found;
    }

    /**
     * פונקציית עזר למציאת סוג האימון המתאים לשריר המבוקש.
     * @param muscle קבוצת השריר שיש לאמן.
     * @return אובייקט Workout התואם לשריר, או null אם לא נמצא כזה במסד הנתונים.
     */
    private Workout findMatchingWorkout(MuscleGroup muscle) {
        for (Workout w : availableWorkouts) {
            if (w.getMuscleGroups().contains(muscle)) {
                return w;
            }
        }
        return null;
    }
}