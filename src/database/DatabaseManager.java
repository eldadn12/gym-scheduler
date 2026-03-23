package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * מחלקה האחראית על ניהול החיבור למסד הנתונים (MySQL).
 * מספקת נקודת גישה מרכזית (Utility Class) ליצירת אובייקטי Connection עבור שאר מחלקות המערכת.
 */
public class DatabaseManager {

    // נתיב ההתחברות - שים לב לשם מסד הנתונים שלנו בסוף
    /** כתובת ה-URL של מסד הנתונים המקומי, כולל הגדרת אזור זמן (UTC) למניעת שגיאות תאימות. */
    private static final String URL = "jdbc:mysql://localhost:3306/gym_schedule_db?serverTimezone=UTC";


    /** שם המשתמש להתחברות למסד הנתונים. */
    private static final String USER = "root";

    /** הסיסמה להתחברות למסד הנתונים. */
    private static final String PASSWORD = "123456";

    /**
     * פונקציה שמחזירה חיבור פעיל למסד הנתונים.
     * יוצרת התקשרות חדשה מול ה-MySQL באמצעות הדרייבר של JDBC והפרטים המוגדרים בקבועי המחלקה.
     * * @return אובייקט Connection המייצג חיבור פתוח למסד הנתונים. יש להקפיד לסגור אותו בסיום השימוש.
     * @throws SQLException נזרקת במידה ויש שגיאת התחברות (למשל שרת ה-MySQL לא רץ, או שהסיסמה שגויה).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}