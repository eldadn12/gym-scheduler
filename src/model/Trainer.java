package model;

import enums.MuscleGroup;
import java.util.List;
import java.util.Objects;

/**
 * מחלקה המייצגת מאמן אישי במערכת.
 * שומרת את פרטי המאמן, ההתמחויות שלו (אילו שרירים הוא מוסמך לאמן), ואת יום החופש שלו.
 * בנוסף, המחלקה מנהלת יומן זמינות פנימי (מערך דו-ממדי) המשמש את אלגוריתם השיבוץ בזמן אמת.
 */
public class Trainer {
    private int id;
    private String name;
    private List<MuscleGroup> specialties;

    // התכונה החדשה מהמסד: היום החופשי הקבוע של המאמן (0 = ראשון, 6 = שבת)
    private int dayOff;

    // יומן פנימי שהאלגוריתם מנהל כדי למנוע כפל שיבוצים באותה שעה
    // לא מגיע מה-DB! מאותחל כריק (false) בכל פעם שמריצים את האלגוריתם
    private boolean[][] bookedSlots;

    /**
     * בנאי לאתחול מאמן חדש במערכת.
     * @param id מזהה המאמן בבסיס הנתונים.
     * @param name שמו המלא של המאמן.
     * @param specialties רשימת קבוצות השרירים שהמאמן מוסמך להעביר בהן אימון.
     * @param dayOff היום החופשי הקבוע של המאמן (0 מייצג את ראשון, 6 את שבת).
     */
    public Trainer(int id, String name, List<MuscleGroup> specialties, int dayOff) {
        this.id = id;
        this.name = name;
        this.specialties = specialties;
        this.dayOff = dayOff;

        // יצירת יומן ריק (7 ימים, 24 שעות)
        this.bookedSlots = new boolean[7][24];
    }

    // ================= Getters =================

    /** @return מזהה המאמן בבסיס הנתונים. */
    public int getId() { return id; }

    /** @return שמו של המאמן. */
    public String getName() { return name; }

    /** @return רשימת ההתמחויות (קבוצות שריר) שהמאמן יודע להעביר. */
    public List<MuscleGroup> getSpecialties() { return specialties; }

    /** @return היום החופשי של המאמן (0-6). */
    public int getDayOff() { return dayOff; }

    // ================= לוגיקת זמינות לאלגוריתם =================

    /**
     * בדיקה האם המאמן פנוי ביום ובשעה המסוימים.
     * @param day היום המבוקש לשיבוץ (0-6).
     * @param hour השעה המבוקשת לשיבוץ (0-23).
     * @return true אם המאמן פנוי (אינו ביום חופש ואינו משובץ לאימון אחר), false אם הוא תפוס.
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
     * פונקציה שהאלגוריתם מפעיל כדי "לתפוס" שעה (שלב ה-Do ב-Backtracking).
     * מסמנת את המשבצת ביומן המאמן כתפוסה.
     * @param day יום השיבוץ.
     * @param hour שעת השיבוץ.
     */
    public void bookSlot(int day, int hour) {
        this.bookedSlots[day][hour] = true;
    }

    /**
     * פונקציה שהאלגוריתם מפעיל כדי לבטל שיבוץ (שלב ה-Undo / Backtrack).
     * משחררת את המשבצת ביומן המאמן בחזרה להיות פנויה.
     * @param day יום השיבוץ שיש לבטל.
     * @param hour שעת השיבוץ שיש לבטל.
     */
    public void freeSlot(int day, int hour) {
        this.bookedSlots[day][hour] = false;
    }

    // ================= Equals & HashCode (חשוב למפות Hash באלגוריתם) =================

    /**
     * השוואה בין מאמנים מתבצעת על בסיס מזהה המאמן (ID) בלבד.
     * @param o האובייקט להשוואה.
     * @return true אם מדובר באותו מאמן, false אחרת.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trainer trainer = (Trainer) o;
        return id == trainer.id;
    }

    /**
     * יצירת קוד Hash ייחודי למאמן מבוסס על ה-ID שלו.
     * @return קוד ה-Hash.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * דריסה של toString כדי להציג את שם המאמן בממשק המשתמש (למשל ב-ComboBox) בצורה תקינה.
     * @return שם המאמן.
     */
    @Override
    public String toString() {
        return this.name;
    }
}