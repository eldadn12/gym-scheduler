package enums;

public enum TrainingGoal {
    STRENGTH("כוח וניפוח (Hypertrophy)"),
    ENDURANCE("סיבולת ואירובי"),
    TONING("חיטוב ועיצוב"),
    GENERAL_HEALTH("כושר כללי ובריאות");

    private final String displayName;

    TrainingGoal(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}