package enums;

/**
 * רמת הכושר של המתאמן.
 * רמת הכושר משפיעה על זמני המנוחה הנדרשים בין אימונים של אותה קבוצת שריר.
 */
public enum FitnessLevel {
    /** מתחיל: צריך 3 ימי מנוחה לשריר (72 שעות) */
    BEGINNER(72),       // מתחיל: צריך 3 ימי מנוחה לשריר
    /** בינוני: צריך יומיים מנוחה (48 שעות) */
    INTERMEDIATE(48),   // בינוני: צריך יומיים מנוחה
    /** מתקדם: מתאושש מהר (24 שעות) */
    ADVANCED(24);       // מתקדם: מתאושש מהר (יכול להתאמן יום אחרי יום במקרים מסוימים)

    private final int requiredRestHours;

    /**
     * בנאי עבור רמת הכושר.
     * @param requiredRestHours מספר שעות המנוחה הנדרשות לשריר לאחר אימון.
     */
    FitnessLevel(int requiredRestHours) {
        this.requiredRestHours = requiredRestHours;
    }

    /**
     * @return מספר שעות המנוחה כשלם.
     */
    public int getRequiredRestHours() {
        return requiredRestHours;
    }
}