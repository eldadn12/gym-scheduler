package model;

import enums.MuscleGroup;
import java.util.List;

public class Assignment {
    private Trainee trainee;
    private Trainer trainer;
    private Workout workout;
    private int day;   // 0-6
    private int hour;  // 9-21

    public Assignment(Trainee trainee, Trainer trainer, Workout workout, int day, int hour) {
        this.trainee = trainee;
        this.trainer = trainer;
        this.workout = workout;
        this.day = day;
        this.hour = hour;
    }

    public Trainee getTrainee() {
        return trainee;
    }

    public void setTrainee(Trainee trainee) {
        this.trainee = trainee;
    }

    public Trainer getTrainer() {
        return trainer;
    }

    public void setTrainer(Trainer trainer) {
        this.trainer = trainer;
    }

    public Workout getWorkout() {
        return workout;
    }

    public void setWorkout(Workout workout) {
        this.workout = workout;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }
}