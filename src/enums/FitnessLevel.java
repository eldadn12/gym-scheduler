package enums;

public enum FitnessLevel {
    BEGINNER(72),       // מתחיל: צריך 3 ימי מנוחה לשריר
    INTERMEDIATE(48),   // בינוני: צריך יומיים מנוחה
    ADVANCED(24);       // מתקדם: מתאושש מהר (יכול להתאמן יום אחרי יום במקרים מסוימים)

    private final int requiredRestHours;

    FitnessLevel(int requiredRestHours) {
        this.requiredRestHours = requiredRestHours;
    }

    public int getRequiredRestHours() {
        return requiredRestHours;
    }
}