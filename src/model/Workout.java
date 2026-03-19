package model;

import enums.MuscleGroup;
import java.util.List;

public class Workout {
    private int id;
    private String name;

    private List<MuscleGroup> muscleGroups; // אילו שרירים עובדים באימון הזה
    private int durationMinutes;            // משך האימון בדקות

    public Workout(int id, String name, List<MuscleGroup> muscleGroups, int durationMinutes) {
        this.id = id;
        this.name = name;
        this.muscleGroups = muscleGroups;
        this.durationMinutes = durationMinutes;
    }
    public int getId() { return id; }
    public String getName() { return name; }
    public List<MuscleGroup> getMuscleGroups() { return muscleGroups; }
    public int getDurationMinutes() { return durationMinutes; }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setMuscleGroups(List<MuscleGroup> muscleGroups) {
        this.muscleGroups = muscleGroups;
    }
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
    @Override
    public String toString() {
        return name + " (" + durationMinutes + " mins)";
    }
}