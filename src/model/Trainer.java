package model;

import enums.MuscleGroup;
import java.util.List;
import java.util.Objects;

/**
 * מחלקה המייצגת מאמן אישי במערכת.
 * כוללת אופטימיזציה של הזיכרון: המערך הדו-ממדי צומצם ל-13 שעות פעילות בלבד (08:00-20:00).
 */
public class Trainer {
    private int id;
    private String name;
    private List<MuscleGroup> specialties;
    private int dayOff;

    // קבועים לניהול ה-Offset של השעות
    private static final int START_HOUR = 8;
    private static final int END_HOUR = 20;
    private static final int HOURS_COUNT = END_HOUR - START_HOUR + 1; // 13 שעות

    // מערך זיכרון מצומצם: 7 ימים, 13 שעות בכל יום
    private boolean[][] bookedSlots;

    /**
     * בנאי לאתחול מאמן חדש במערכת.
     */
    public Trainer(int id, String name, List<MuscleGroup> specialties, int dayOff) {
        this.id = id;
        this.name = name;
        this.specialties = specialties;
        this.dayOff = dayOff;

        // אתחול מערך מצומצם לחיסכון בזיכרון
        this.bookedSlots = new boolean[7][HOURS_COUNT];
    }

    // ================= Getters =================

    public int getId() { return id; }
    public String getName() { return name; }
    public List<MuscleGroup> getSpecialties() { return specialties; }
    public int getDayOff() { return dayOff; }

    // ================= לוגיקת זמינות עם Offset =================

    /**
     * בדיקה האם המאמן פנוי ביום ובשעה המסוימים.
     */
    public boolean isAvailable(int day, int hour) {
        // בדיקת גבולות - אם השעה מחוץ לטווח הפעילות (8-20) הוא לא פנוי
        if (hour < START_HOUR || hour > END_HOUR) {
            return false;
        }

        // 1. האם זה היום החופשי שלו?
        if (day == this.dayOff) {
            return false;
        }

        // 2. בדיקה במערך המצומצם באמצעות היסט (hour - 8)
        return !this.bookedSlots[day][hour - START_HOUR];
    }

    /**
     * סימון משבצת כתפוסה (שלב ה-Do ב-Backtracking).
     */
    public void bookSlot(int day, int hour) {
        if (hour >= START_HOUR && hour <= END_HOUR) {
            this.bookedSlots[day][hour - START_HOUR] = true;
        }
    }

    /**
     * שחרור משבצת (שלב ה-Undo ב-Backtracking).
     */
    public void freeSlot(int day, int hour) {
        if (hour >= START_HOUR && hour <= END_HOUR) {
            this.bookedSlots[day][hour - START_HOUR] = false;
        }
    }

    // ================= Equals & HashCode =================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trainer trainer = (Trainer) o;
        return id == trainer.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return this.name;
    }
}