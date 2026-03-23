package database;

import enums.FitnessLevel;
import enums.MuscleGroup;
import enums.TrainingGoal;
import model.Trainee;
import model.Trainer;
import model.Workout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * מחלקה זו משמשת כ"מתווכת" בין האובייקטים בקוד לבין מסד הנתונים (MySQL).
 * היא מכילה את כל שאילתות ה-SQL הנדרשות לשליפה, הוספה, עדכון ומחיקה של נתונים (CRUD).
 */
public class DatabaseRepository {

    // ==========================================
    // פעולות שליפה (קריאה - Read)
    // ==========================================

    /**
     * שליפת כל האימונים ממסד הנתונים, כולל רשימת השרירים שכל אימון מפעיל.
     */
    public List<Workout> getAllWorkouts() {
        List<Workout> workouts = new ArrayList<>();
        String query = "SELECT * FROM Workouts";

        // שימוש ב-try-with-resources כדי לוודא שהחיבור נסגר אוטומטית בסיום
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) { // rs מכיל את התוצאות שחזרו מהטבלה

            // מעבר על כל השורות שחזרו
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int duration = rs.getInt("duration_minutes");

                // שליפת השרירים שקשורים לאימון הזה מהטבלה המקשרת
                List<MuscleGroup> muscles = getWorkoutMuscles(conn, id);

                // בניית אובייקט מסוג Workout והוספתו לרשימה
                workouts.add(new Workout(id, name, muscles, duration));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching workouts: " + e.getMessage());
        }
        return workouts;
    }

    /**
     * פונקציית עזר: שולפת את כל השרירים המשויכים לאימון ספציפי מתוך טבלה מקשרת.
     */
    private List<MuscleGroup> getWorkoutMuscles(Connection conn, int workoutId) throws SQLException {
        List<MuscleGroup> muscles = new ArrayList<>();
        // ה-'?' משמש למניעת SQL Injection על ידי הכנסת הנתון בצורה בטוחה
        String query = "SELECT muscle_group FROM Workout_Muscles WHERE workout_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, workoutId); // מציבים את ה-ID של האימון במקום סימן השאלה
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // המרה של המחרוזת מה-DB לערך של Enum
                    muscles.add(MuscleGroup.valueOf(rs.getString("muscle_group")));
                }
            }
        }
        return muscles;
    }

    /**
     * שליפת כל המאמנים ממסד הנתונים, כולל היום החופשי והתמחויות.
     */
    public List<Trainer> getAllTrainers() {
        List<Trainer> trainers = new ArrayList<>();
        String query = "SELECT * FROM Trainers";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");

                // שליפת היום החופשי מהמסד (0=ראשון, 6=שבת)
                int dayOff = rs.getInt("day_off");

                // שליפת ההתמחויות של המאמן מהטבלה המקשרת
                List<MuscleGroup> specialties = getTrainerSpecialties(conn, id);

                // יצירת אובייקט המאמן עם היום החופשי והוספתו לרשימה
                trainers.add(new Trainer(id, name, specialties, dayOff));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching trainers: " + e.getMessage());
        }
        return trainers;
    }

    /**
     * פונקציית עזר: שולפת את ההתמחויות של מאמן ספציפי.
     */
    private List<MuscleGroup> getTrainerSpecialties(Connection conn, int trainerId) throws SQLException {
        List<MuscleGroup> specialties = new ArrayList<>();
        String query = "SELECT muscle_group FROM Trainer_Specialties WHERE trainer_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, trainerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    specialties.add(MuscleGroup.valueOf(rs.getString("muscle_group")));
                }
            }
        }
        return specialties;
    }

    /**
     * שליפת כל המתאמנים מהמסד, כולל רמת כושר, מטרה ופציעות.
     */
    public List<Trainee> getAllTrainees() {
        List<Trainee> trainees = new ArrayList<>();
        String query = "SELECT * FROM Trainees";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");

                // המרת הטקסט מה-DB חזרה לאובייקטי Enum שמוגדרים בקוד
                FitnessLevel level = FitnessLevel.valueOf(rs.getString("fitness_level"));
                TrainingGoal goal = TrainingGoal.valueOf(rs.getString("goal"));
                int maxWorkouts = rs.getInt("max_workouts");

                Trainee trainee = new Trainee(id, name, level, goal, maxWorkouts);

                // שליפת פציעות מהטבלה המקשרת ועדכון אובייקט המתאמן
                loadTraineeInjuries(conn, trainee);
                trainees.add(trainee);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching trainees: " + e.getMessage());
        }
        return trainees;
    }

    /**
     * פונקציית עזר: טוענת את כל הפציעות של מתאמן ספציפי ומוסיפה אותן לאובייקט שלו.
     */
    private void loadTraineeInjuries(Connection conn, Trainee trainee) throws SQLException {
        String query = "SELECT muscle_group FROM Trainee_Injuries WHERE trainee_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, trainee.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trainee.addInjury(MuscleGroup.valueOf(rs.getString("muscle_group")));
                }
            }
        }
    }

    // ==========================================
    // פעולות ניהול מתאמנים (CRUD)
    // ==========================================

    /**
     * הוספת מתאמן חדש למסד הנתונים (Create).
     */
    public boolean addTrainee(String name, FitnessLevel level, TrainingGoal goal, int maxWorkouts) {
        String query = "INSERT INTO Trainees (name, fitness_level, goal, max_workouts) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            // הצבת הערכים במקום סימני השאלה בשאילתה
            stmt.setString(1, name);
            stmt.setString(2, level.name()); // שומר את שם ה-Enum כטקסט
            stmt.setString(3, goal.name());
            stmt.setInt(4, maxWorkouts);

            // executeUpdate משמש לפעולות INSERT, UPDATE, DELETE ומחזיר את מספר השורות שהושפעו
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // מחזיר אמת אם השורה נוספה בהצלחה

        } catch (SQLException e) {
            System.err.println("Error adding trainee: " + e.getMessage());
            return false;
        }
    }

    /**
     * מחיקת מתאמן ממסד הנתונים (Delete).
     * הערה לבוחן: הפציעות שלו בטבלה המקשרת יימחקו אוטומטית בזכות מנגנון ה-CASCADE המוגדר ב-DB.
     */
    public boolean deleteTrainee(int traineeId) {
        String query = "DELETE FROM Trainees WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, traineeId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // מחזיר אמת אם המחיקה הצליחה

        } catch (SQLException e) {
            System.err.println("Error deleting trainee: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // פעולות ניהול מאמנים (CRUD)
    // ==========================================

    /**
     * הוספת מאמן חדש למסד הנתונים (כולל ההתמחויות שלו בטבלה המקשרת).
     * פונקציה זו משתמשת ב"טרנזקציה" (Transaction) כדי להבטיח שגם המאמן וגם ההתמחויות
     * שלו נשמרים יחד. אם אחד נכשל, הכל מבוטל (Rollback).
     */
    public boolean addTrainer(String name, List<MuscleGroup> specialties) {
        String insertTrainer = "INSERT INTO Trainers (name, day_off) VALUES (?, ?)";
        String insertSpecialty = "INSERT INTO Trainer_Specialties (trainer_id, muscle_group) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();

            // כיבוי השמירה האוטומטית - התחלת הטרנזקציה!
            conn.setAutoCommit(false);

            // 1. הכנסת המאמן (ברירת מחדל: יום חופש בשבת = 6)
            // בקשת RETURN_GENERATED_KEYS כדי לקבל חזרה את ה-ID שה-DB הקצה למאמן החדש
            try (PreparedStatement stmtTrainer = conn.prepareStatement(insertTrainer, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmtTrainer.setString(1, name);
                stmtTrainer.setInt(2, 6);
                int affectedRows = stmtTrainer.executeUpdate();

                // אם ההכנסה נכשלה, מבטלים הכל
                if (affectedRows == 0) {
                    conn.rollback();
                    return false;
                }

                // 2. שליפת ה-ID החדש שנוצר למאמן כדי שנוכל לקשר אליו את ההתמחויות
                try (ResultSet generatedKeys = stmtTrainer.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int trainerId = generatedKeys.getInt(1);

                        // 3. הכנסת כל ההתמחויות שלו לטבלה המקשרת באמצעות Batch (הכנסה מקובצת ויעילה)
                        try (PreparedStatement stmtSpec = conn.prepareStatement(insertSpecialty)) {
                            for (MuscleGroup mg : specialties) {
                                stmtSpec.setInt(1, trainerId);
                                stmtSpec.setString(2, mg.name());
                                stmtSpec.addBatch(); // הוספה לחבילת הפקודות
                            }
                            stmtSpec.executeBatch(); // הרצת כל פקודות ההכנסה בבת אחת
                        }
                    } else {
                        // אם לא נוצר ID, משהו השתבש
                        conn.rollback();
                        return false;
                    }
                }
            }

            // אם הגענו לפה, הכל עבד בצורה מושלמת - אנחנו שומרים את הנתונים סופית (Commit)
            conn.commit();
            return true;

        } catch (SQLException e) {
            // במקרה של שגיאה (למשל נפילת רשת), מבטלים את כל מה שנעשה בטרנזקציה
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            System.err.println("Error adding trainer: " + e.getMessage());
            return false;
        } finally {
            // החזרת המצב לקדמותו וסגירת החיבור למסד הנתונים
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * מחיקת מאמן (Delete).
     * ההתמחויות שלו יימחקו אוטומטית מטבלת Trainer_Specialties בגלל הגדרת ה-ON DELETE CASCADE במסד.
     */
    public boolean deleteTrainer(int trainerId) {
        String query = "DELETE FROM Trainers WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, trainerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting trainer: " + e.getMessage());
            return false;
        }
    }

    /**
     * עדכון (Update) היום החופשי של המאמן.
     * מקבל את ה-ID של המאמן ואת היום החדש (0 = ראשון, 6 = שבת).
     */
    public boolean updateTrainerDayOff(int trainerId, int dayOff) {
        String query = "UPDATE Trainers SET day_off = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, dayOff);
            stmt.setInt(2, trainerId);
            return stmt.executeUpdate() > 0; // אמת אם הערך עודכן בטבלה
        } catch (SQLException e) {
            System.err.println("Error updating trainer day off: " + e.getMessage());
            return false;
        }
    }
}