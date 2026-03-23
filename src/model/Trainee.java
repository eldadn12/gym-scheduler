package model;

import enums.FitnessLevel;
import enums.MuscleGroup;
import enums.TrainingGoal;

import java.util.*;

/**
 * מחלקה המייצגת מתאמן במערכת.
 * מעבר לשמירת נתונים, המחלקה מנהלת את מצב המתאמן במהלך ריצת אלגוריתם ה-Backtracking,
 * כולל מעקב אחר עומס אימונים, פציעות, וזמני התאוששות לשרירים (Rest Time).
 */
public class Trainee {
    private int id;
    private String name;
    private FitnessLevel fitnessLevel; // רמת הכושר (משפיעה על זמן ההתאוששות הנדרש)
    private TrainingGoal goal;         // מטרת האימון (כוח, חיטוב וכו')
    private int maxWorkoutsPerWeek;    // מגבלת אימונים שבועית

    // סט לשמירת שרירים פצועים (חיפוש ב-O(1) כדי למנוע שיבוץ אימונים על שריר פצוע)
    private Set<MuscleGroup> injuredMuscles;

    // מפת היסטוריית אימונים: ממפה כל קבוצת שריר לרשימת הימים שבהם השריר הזה אומן.
    // משמש את האלגוריתם כדי לוודא שעבר מספיק זמן התאוששות.
    private Map<MuscleGroup, List<Integer>> trainedMusclesHistory;

    // מונה המעקב אחר כמות האימונים שכבר שובצו למתאמן בשבוע הנוכחי
    private int currentWorkoutCount = 0;

    /**
     * בנאי לאתחול מתאמן חדש במערכת.
     * @param id מזהה המתאמן בבסיס הנתונים.
     * @param name השם המלא של המתאמן.
     * @param fitnessLevel רמת הכושר של המתאמן (קובעת את זמני המנוחה).
     * @param goal מטרת האימון המרכזית שלו (קובעת את השרירים שיאמן).
     * @param maxWorkoutsPerWeek מגבלת כמות האימונים שהמתאמן יכול לבצע בשבוע.
     */
    public Trainee(int id, String name, FitnessLevel fitnessLevel, TrainingGoal goal, int maxWorkoutsPerWeek) {
        this.id = id;
        this.name = name;
        this.fitnessLevel = fitnessLevel;
        this.goal = goal;
        this.maxWorkoutsPerWeek = maxWorkoutsPerWeek;
        this.injuredMuscles = new HashSet<>();
        this.trainedMusclesHistory = new HashMap<>();
    }

    /**
     * הוספת שריר לרשימת הפציעות של המתאמן.
     * @param muscle קבוצת השריר הפצועה שיש להימנע מלאמן אותה.
     */
    public void addInjury(MuscleGroup muscle) {
        this.injuredMuscles.add(muscle);
    }

    /**
     * פונקציית אילוצים (Constraint) קריטית לאלגוריתם.
     * בודקת האם המתאמן יכול לאמן שריר מסוים ביום ספציפי בהתאם למגבלות.
     * @param muscle קבוצת השריר המיועדת לאימון.
     * @param currentDay היום בשבוע בו מתוכנן האימון (0-6).
     * @return true אם המתאמן כשיר לאמן שריר זה ביום המבוקש, false אחרת.
     */
    public boolean canTrainMuscle(MuscleGroup muscle, int currentDay) {
        // 1. חיתוך ענף: אם השריר פצוע, אי אפשר לאמן אותו בכלל
        if (injuredMuscles.contains(muscle)) return false;

        // 2. חיתוך ענף: אם המתאמן כבר הגיע למכסת האימונים השבועית שלו
        if (currentWorkoutCount >= maxWorkoutsPerWeek) return false;

        // 3. בדיקת זמן התאוששות (Rest Days)
        if (trainedMusclesHistory.containsKey(muscle) && !trainedMusclesHistory.get(muscle).isEmpty()) {
            List<Integer> history = trainedMusclesHistory.get(muscle);
            int lastDay = history.get(history.size() - 1); // שליפת היום האחרון שבו השריר אומן

            // המרת שעות המנוחה הנדרשות (לפי רמת הכושר) לימים
            int requiredRestDays = fitnessLevel.getRequiredRestHours() / 24;

            // אם ההפרש בין היום הנוכחי ליום האימון הקודם קטן או שווה לימי המנוחה הנדרשים -> השריר עייף מדי
            if ((currentDay - lastDay) <= requiredRestDays) {
                return false;
            }
        }
        return true; // המתאמן כשיר לאמן שריר זה היום!
    }

    /**
     * פונקציית "עשה" (DO) עבור אלגוריתם ה-Backtracking.
     * כאשר האלגוריתם מחליט לשבץ אימון, הוא מעדכן את היסטוריית השרירים ומעלה את מונה האימונים.
     * @param muscle קבוצת השריר שאומנה זה עתה.
     * @param day היום בשבוע שבו שובץ האימון.
     */
    public void updateMuscleTraining(MuscleGroup muscle, int day) {
        trainedMusclesHistory.putIfAbsent(muscle, new ArrayList<>());
        trainedMusclesHistory.get(muscle).add(day);
        currentWorkoutCount++;
    }

    /**
     * פונקציית "בטל" (UNDO / Backtrack) עבור אלגוריתם ה-Backtracking.
     * כאשר האלגוריתם מגיע למבוי סתום וחוזר אחורה (נסיגה), הוא חייב לבטל את הרישום של האימון
     * האחרון שבוצע לאותו שריר, ולהוריד את מונה האימונים הכללי.
     * @param muscle קבוצת השריר שיש למחוק עבורה את השיבוץ האחרון.
     */
    public void undoMuscleTraining(MuscleGroup muscle) {
        List<Integer> days = trainedMusclesHistory.get(muscle);
        if (days != null && !days.isEmpty()) {
            days.remove(days.size() - 1); // מחיקת היום האחרון שנוסף
            currentWorkoutCount--; // עדכון מונה האימונים חזרה למטה
        }
    }

    /**
     * פונקציה המתרגמת את מטרת העל של המתאמן (למשל "חיטוב")
     * לרשימה פרקטית של קבוצות שרירים שהאלגוריתם צריך לשבץ לו במהלך השבוע.
     * @return רשימה של קבוצות שריר להן המתאמן זקוק, ללא שרירים פצועים.
     */
    public List<MuscleGroup> getRequiredWorkouts() {
        // שימוש ב-Switch Expression (פיצ'ר מתקדם ב-Java) כדי לבנות את הרשימה במהירות
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

        // סינון חכם: מסיר אוטומטית מהדרישות השבועיות כל שריר שמופיע ברשימת הפציעות!
        requirements.removeIf(injuredMuscles::contains);

        return requirements;
    }

    // ================= Getters =================

    /** @return מזהה המתאמן. */
    public int getId() { return id; }

    /** @return שם המתאמן. */
    public String getName() { return name; }

    /** @return רמת הכושר של המתאמן. */
    public FitnessLevel getFitnessLevel() { return fitnessLevel; }

    /** @return מטרת האימון של המתאמן. */
    public TrainingGoal getGoal() { return goal; }

    /** @return מגבלת האימונים השבועית. */
    public int getMaxWorkoutsPerWeek() { return maxWorkoutsPerWeek; }

    /** @return קבוצת השרירים הפצועים של המתאמן. */
    public Set<MuscleGroup> getInjuredMuscles() { return injuredMuscles; }

    /**
     * דריסה של toString כדי שכאשר נציג את המתאמן ברשימות נגללות (ComboBox) בממשק המשתמש,
     * יופיע השם שלו בצורה נקייה ולא כתובת זיכרון.
     * @return שם המתאמן.
     */
    @Override
    public String toString() {
        return this.name;
    }
}