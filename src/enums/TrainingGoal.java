package enums;

/**
 * מטרת האימון המרכזית של המתאמן.
 */
public enum TrainingGoal {
    /** כוח וניפוח מסת שריר */
    STRENGTH("כוח וניפוח (Hypertrophy)"),
    /** סיבולת ואירובי */
    ENDURANCE("סיבולת ואירובי"),
    /** חיטוב ואחוזי שומן */
    TONING("חיטוב ועיצוב"),
    /** כושר כללי ובריאות */
    GENERAL_HEALTH("כושר כללי ובריאות");

    private final String displayName;

    /**
     * בנאי למטרת האימון.
     * @param displayName השם התצוגתי של המטרה.
     */
    TrainingGoal(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return מחרוזת המייצגת את שם המטרה.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return ייצוג טקסטואלי של המטרה לטובת הצגה בממשק.
     */
    @Override
    public String toString() {
        return displayName;
    }
}