package model;

import enums.MuscleGroup;
import java.util.List;

/**
 * מחלקה המייצגת סוג אימון (Workout) אפשרי במערכת.
 * המחלקה שומרת את פרטי האימון, את משכו בדקות, ואת קבוצות השרירים הספציפיות המופעלות במהלך אימון זה.
 */
public class Workout {
    private int id;
    private String name;

    private List<MuscleGroup> muscleGroups; // אילו שרירים עובדים באימון הזה
    private int durationMinutes;            // משך האימון בדקות

    /**
     * בנאי לאתחול סוג אימון חדש במערכת.
     * @param id המזהה הייחודי של האימון בבסיס הנתונים.
     * @param name השם של האימון (למשל: "אימון משקולות", "פילאטיס").
     * @param muscleGroups רשימת קבוצות השרירים שמופעלות במהלך האימון.
     * @param durationMinutes משך האימון בדקות.
     */
    public Workout(int id, String name, List<MuscleGroup> muscleGroups, int durationMinutes) {
        this.id = id;
        this.name = name;
        this.muscleGroups = muscleGroups;
        this.durationMinutes = durationMinutes;
    }

    /**
     * @return המזהה הייחודי של האימון (ID).
     */
    public int getId() { return id; }

    /**
     * @return שם האימון.
     */
    public String getName() { return name; }

    /**
     * @return רשימת קבוצות השרירים שעליהן עובדים באימון זה.
     */
    public List<MuscleGroup> getMuscleGroups() { return muscleGroups; }

    /**
     * @return משך האימון בדקות.
     */
    public int getDurationMinutes() { return durationMinutes; }

    /**
     * @param id המזהה החדש לעדכון עבור אימון זה.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @param name השם החדש לעדכון.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param muscleGroups רשימת שרירים חדשה לעדכון.
     */
    public void setMuscleGroups(List<MuscleGroup> muscleGroups) {
        this.muscleGroups = muscleGroups;
    }

    /**
     * @param durationMinutes משך אימון חדש בדקות לעדכון.
     */
    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    /**
     * דריסה של מתודת toString להצגה נוחה של האימון בממשק המשתמש (למשל בתוך הכרטיסיות בלוח הראשי).
     * @return מחרוזת המכילה את שם האימון ומשכו בדקות.
     */
    @Override
    public String toString() {
        return name + " (" + durationMinutes + " mins)";
    }
}