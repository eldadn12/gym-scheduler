package model;

import enums.MuscleGroup;
import java.util.List;

/**
 * מחלקה המייצגת שיבוץ סופי במערכת (משבצת זמן בלוח השעות).
 * אובייקט זה מקשר בין כל הגורמים: המתאמן, המאמן, סוג האימון והתזמון המדויק (יום ושעה).
 */
public class Assignment {
    // אובייקטי המשתתפים והאימון
    private Trainee trainee;
    private Trainer trainer;
    private Workout workout;

    // נתוני הזמן של השיבוץ
    private int day;   // 0-6
    private int hour;  // 9-21

    /**
     * בנאי ליצירת שיבוץ חדש בלוח השעות.
     * @param trainee המתאמן ששובץ לאימון.
     * @param trainer המאמן שמעביר את האימון.
     * @param workout סוג האימון שהוקצה.
     * @param day היום בשבוע שבו מתקיים האימון (0 מייצג את יום ראשון, 6 מייצג את שבת).
     * @param hour השעה שבה מתחיל האימון (בפורמט 24 שעות, למשל מ-9 עד 21).
     */
    public Assignment(Trainee trainee, Trainer trainer, Workout workout, int day, int hour) {
        this.trainee = trainee;
        this.trainer = trainer;
        this.workout = workout;
        this.day = day;
        this.hour = hour;
    }

    /**
     * @return אובייקט המתאמן המשובץ.
     */
    public Trainee getTrainee() {
        return trainee;
    }

    /**
     * @param trainee המתאמן החדש לעדכון בשיבוץ זה.
     */
    public void setTrainee(Trainee trainee) {
        this.trainee = trainee;
    }

    /**
     * @return אובייקט המאמן המשובץ להעביר את האימון.
     */
    public Trainer getTrainer() {
        return trainer;
    }

    /**
     * @param trainer המאמן החדש לעדכון בשיבוץ זה.
     */
    public void setTrainer(Trainer trainer) {
        this.trainer = trainer;
    }

    /**
     * @return אובייקט סוג האימון שנקבע (Workout).
     */
    public Workout getWorkout() {
        return workout;
    }

    /**
     * @param workout סוג האימון החדש לעדכון.
     */
    public void setWorkout(Workout workout) {
        this.workout = workout;
    }

    /**
     * @return הערך המספרי של היום בשבוע בו נקבע האימון (0-6).
     */
    public int getDay() {
        return day;
    }

    /**
     * @param day היום החדש בשבוע לעדכון (0-6).
     */
    public void setDay(int day) {
        this.day = day;
    }

    /**
     * @return השעה שבה מתקיים האימון (9-21).
     */
    public int getHour() {
        return hour;
    }

    /**
     * @param hour השעה החדשה לעדכון.
     */
    public void setHour(int hour) {
        this.hour = hour;
    }
}