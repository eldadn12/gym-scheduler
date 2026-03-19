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

public class DatabaseRepository {

    // שליפת כל האימונים מהמסד
    public List<Workout> getAllWorkouts() {
        List<Workout> workouts = new ArrayList<>();
        String query = "SELECT * FROM Workouts";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int duration = rs.getInt("duration_minutes");

                // שליפת השרירים שקשורים לאימון הזה מהטבלה המקשרת
                List<MuscleGroup> muscles = getWorkoutMuscles(conn, id);
                workouts.add(new Workout(id, name, muscles, duration));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching workouts: " + e.getMessage());
        }
        return workouts;
    }

    private List<MuscleGroup> getWorkoutMuscles(Connection conn, int workoutId) throws SQLException {
        List<MuscleGroup> muscles = new ArrayList<>();
        String query = "SELECT muscle_group FROM Workout_Muscles WHERE workout_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, workoutId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    muscles.add(MuscleGroup.valueOf(rs.getString("muscle_group")));
                }
            }
        }
        return muscles;
    }

    // שליפת כל המאמנים מהמסד
    public List<Trainer> getAllTrainers() {
        List<Trainer> trainers = new ArrayList<>();
        String query = "SELECT * FROM Trainers";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");

                // --- השינוי שלנו: שליפת היום החופשי מהמסד ---
                int dayOff = rs.getInt("day_off");

                // שליפת ההתמחויות מהטבלה המקשרת
                List<MuscleGroup> specialties = getTrainerSpecialties(conn, id);

                // --- השינוי שלנו: העברת היום החופשי לבנאי של המאמן ---
                trainers.add(new Trainer(id, name, specialties, dayOff));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching trainers: " + e.getMessage());
        }
        return trainers;
    }

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

    // שליפת כל המתאמנים מהמסד
    public List<Trainee> getAllTrainees() {
        List<Trainee> trainees = new ArrayList<>();
        String query = "SELECT * FROM Trainees";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                FitnessLevel level = FitnessLevel.valueOf(rs.getString("fitness_level"));
                TrainingGoal goal = TrainingGoal.valueOf(rs.getString("goal"));
                int maxWorkouts = rs.getInt("max_workouts");

                Trainee trainee = new Trainee(id, name, level, goal, maxWorkouts);

                // שליפת פציעות מהטבלה המקשרת
                loadTraineeInjuries(conn, trainee);
                trainees.add(trainee);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching trainees: " + e.getMessage());
        }
        return trainees;
    }

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
     * הוספת מתאמן חדש למסד הנתונים
     */
    public boolean addTrainee(String name, FitnessLevel level, TrainingGoal goal, int maxWorkouts) {
        String query = "INSERT INTO Trainees (name, fitness_level, goal, max_workouts) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, name);
            stmt.setString(2, level.name()); // שומר את שם ה-Enum
            stmt.setString(3, goal.name());
            stmt.setInt(4, maxWorkouts);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0; // מחזיר אמת אם השורה נוספה בהצלחה

        } catch (SQLException e) {
            System.err.println("Error adding trainee: " + e.getMessage());
            return false;
        }
    }

    /**
     * מחיקת מתאמן ממסד הנתונים (הפציעות שלו יימחקו אוטומטית בזכות ה-CASCADE ב-SQL)
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
     * הוספת מאמן חדש למסד הנתונים (כולל ההתמחויות שלו בטבלה המקשרת)
     */
    public boolean addTrainer(String name, List<MuscleGroup> specialties) {
        String insertTrainer = "INSERT INTO Trainers (name, day_off) VALUES (?, ?)";
        String insertSpecialty = "INSERT INTO Trainer_Specialties (trainer_id, muscle_group) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // מתחילים טרנזקציה כדי לוודא ששתי הטבלאות מתעדכנות יחד

            // 1. הכנסת המאמן (ברירת מחדל: יום חופש בשבת = 6)
            try (PreparedStatement stmtTrainer = conn.prepareStatement(insertTrainer, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmtTrainer.setString(1, name);
                stmtTrainer.setInt(2, 6);
                int affectedRows = stmtTrainer.executeUpdate();

                if (affectedRows == 0) {
                    conn.rollback();
                    return false;
                }

                // 2. שליפת ה-ID החדש שנוצר למאמן
                try (ResultSet generatedKeys = stmtTrainer.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int trainerId = generatedKeys.getInt(1);

                        // 3. הכנסת כל ההתמחויות שלו לטבלה המקשרת
                        try (PreparedStatement stmtSpec = conn.prepareStatement(insertSpecialty)) {
                            for (MuscleGroup mg : specialties) {
                                stmtSpec.setInt(1, trainerId);
                                stmtSpec.setString(2, mg.name());
                                stmtSpec.addBatch();
                            }
                            stmtSpec.executeBatch();
                        }
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }
            conn.commit(); // אישור סופי של הטרנזקציה
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            System.err.println("Error adding trainer: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * מחיקת מאמן (ההתמחויות יימחקו אוטומטית בגלל ה-CASCADE ב-DB)
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
     * עדכון היום החופשי של המאמן
     */
    public boolean updateTrainerDayOff(int trainerId, int dayOff) {
        String query = "UPDATE Trainers SET day_off = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, dayOff);
            stmt.setInt(2, trainerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating trainer day off: " + e.getMessage());
            return false;
        }
    }
}