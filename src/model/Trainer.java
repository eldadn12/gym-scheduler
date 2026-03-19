package model;

import enums.MuscleGroup;
import java.util.List;
import java.util.Objects;

public class Trainer {
    private int id;
    private String name;
    private List<MuscleGroup> specialties;

    // התכונה החדשה מהמסד: היום החופשי הקבוע של המאמן (0 = ראשון, 6 = שבת)
    private int dayOff;

    // יומן פנימי שהאלגוריתם מנהל כדי למנוע כפל שיבוצים באותה שעה
    // לא מגיע מה-DB! מאותחל כריק (false) בכל פעם שמריצים את האלגוריתם
    private boolean[][] bookedSlots;

    public Trainer(int id, String name, List<MuscleGroup> specialties, int dayOff) {
        this.id = id;
        this.name = name;
        this.specialties = specialties;
        this.dayOff = dayOff;

        // יצירת יומן ריק (7 ימים, 24 שעות)
        this.bookedSlots = new boolean[7][24];
    }

    // ================= Getters =================

    public int getId() { return id; }
    public String getName() { return name; }
    public List<MuscleGroup> getSpecialties() { return specialties; }
    public int getDayOff() { return dayOff; }

    // ================= לוגיקת זמינות לאלגוריתם =================

    /**
     * בדיקה האם המאמן פנוי ביום ובשעה המסוימים
     */
    public boolean isAvailable(int day, int hour) {
        // 1. האם זה היום החופשי שלו מה-DB?
        if (day == this.dayOff) {
            return false;
        }

        // 2. האם האלגוריתם כבר קבע לו אימון בשעה הזו?
        if (this.bookedSlots[day][hour]) {
            return false; // השעה הזו כבר שובצה!
        }

        return true; // המאמן פנוי
    }

    /**
     * פונקציה שהאלגוריתם מפעיל כדי "לתפוס" שעה (שלב ה-Do)
     */
    public void bookSlot(int day, int hour) {
        this.bookedSlots[day][hour] = true;
    }

    /**
     * פונקציה שהאלגוריתם מפעיל כדי לבטל שיבוץ (שלב ה-Undo / Backtrack)
     */
    public void freeSlot(int day, int hour) {
        this.bookedSlots[day][hour] = false;
    }

    // ================= Equals & HashCode (חשוב למפות Hash באלגוריתם) =================
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