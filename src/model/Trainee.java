package model;

import enums.FitnessLevel;
import enums.MuscleGroup;
import enums.TrainingGoal;

import java.util.*;

public class Trainee {
    private int id;
    private String name;
    private FitnessLevel fitnessLevel;
    private TrainingGoal goal;
    private int maxWorkoutsPerWeek;
    private Set<MuscleGroup> injuredMuscles;
    private Map<MuscleGroup, List<Integer>> trainedMusclesHistory;
    private int currentWorkoutCount = 0;

    public Trainee(int id, String name, FitnessLevel fitnessLevel, TrainingGoal goal, int maxWorkoutsPerWeek) {
        this.id = id;
        this.name = name;
        this.fitnessLevel = fitnessLevel;
        this.goal = goal;
        this.maxWorkoutsPerWeek = maxWorkoutsPerWeek;
        this.injuredMuscles = new HashSet<>();
        this.trainedMusclesHistory = new HashMap<>();
    }

    public void addInjury(MuscleGroup muscle) {
        this.injuredMuscles.add(muscle);
    }

    public boolean canTrainMuscle(MuscleGroup muscle, int currentDay) {
        if (injuredMuscles.contains(muscle)) return false;
        if (currentWorkoutCount >= maxWorkoutsPerWeek) return false;

        if (trainedMusclesHistory.containsKey(muscle) && !trainedMusclesHistory.get(muscle).isEmpty()) {
            List<Integer> history = trainedMusclesHistory.get(muscle);
            int lastDay = history.get(history.size() - 1);
            int requiredRestDays = fitnessLevel.getRequiredRestHours() / 24;

            if ((currentDay - lastDay) <= requiredRestDays) {
                return false;
            }
        }
        return true;
    }

    public void updateMuscleTraining(MuscleGroup muscle, int day) {
        trainedMusclesHistory.putIfAbsent(muscle, new ArrayList<>());
        trainedMusclesHistory.get(muscle).add(day);
        currentWorkoutCount++;
    }

    public void undoMuscleTraining(MuscleGroup muscle) {
        List<Integer> days = trainedMusclesHistory.get(muscle);
        if (days != null && !days.isEmpty()) {
            days.remove(days.size() - 1);
            currentWorkoutCount--; //
        }
    }

    public List<MuscleGroup> getRequiredWorkouts() {
        List<MuscleGroup> requirements = switch (this.goal) {
            case STRENGTH -> new ArrayList<>(Arrays.asList(
                    MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.LEGS,
                    MuscleGroup.SHOULDERS, MuscleGroup.FULL_BODY));

            case TONING -> new ArrayList<>(Arrays.asList(
                    MuscleGroup.FULL_BODY, MuscleGroup.CARDIO, MuscleGroup.CORE));

            case ENDURANCE -> new ArrayList<>(Arrays.asList(
                    MuscleGroup.CARDIO, MuscleGroup.CORE, MuscleGroup.LEGS));

            case GENERAL_HEALTH -> new ArrayList<>(Arrays.asList(
                    MuscleGroup.FULL_BODY, MuscleGroup.CARDIO));
        };

        // סינון פציעות
        requirements.removeIf(injuredMuscles::contains);

        return requirements;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public FitnessLevel getFitnessLevel() { return fitnessLevel; }
    public TrainingGoal getGoal() { return goal; }
    public int getMaxWorkoutsPerWeek() { return maxWorkoutsPerWeek; }
    public Set<MuscleGroup> getInjuredMuscles() { return injuredMuscles; }

    @Override
    public String toString() {
        return this.name;
    }
}