package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    // נתיב ההתחברות - שים לב לשם מסד הנתונים שלנו בסוף
    private static final String URL = "jdbc:mysql://localhost:3306/gym_schedule_db?serverTimezone=UTC";


    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    /**
     * פונקציה שמחזירה חיבור פעיל למסד הנתונים
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}