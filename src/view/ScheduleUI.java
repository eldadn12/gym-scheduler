package view;

import algorithm.ScheduleSolver;
import database.DatabaseRepository;
import model.*;
import enums.*;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * המחלקה הראשית של הממשק הגרפי.
 * ב-JavaFX, כל חלון יורש מהמחלקה Application.
 */
public class ScheduleUI extends Application {

    // קבועים להגדרת גודל הטבלה (7 ימים, 13 שעות פעילות מ-08:00 עד 20:00)
    private static final int DAYS_COUNT = 7;
    private static final int HOURS_COUNT = 13;
    private static final String[] DAY_NAMES = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};

    // צבעים ועיצובים (CSS) שנשמרים במחרוזות קבועות כדי לשמור על קוד נקי וקריא
    private static final String HEADER_STYLE = "-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: center; -fx-font-size: 14px; -fx-border-color: #34495e;";
    private static final String HOUR_COLUMN_STYLE = "-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-alignment: center; -fx-border-color: #bdc3c7;";
    private static final String CELL_STYLE = "-fx-background-color: white; -fx-border-color: #dfe6e9; -fx-border-width: 0.5;";
    private static final String ASSIGNMENT_CARD_STYLE = "-fx-background-color: #74b9ff; -fx-background-radius: 5; -fx-border-color: #0984e3; -fx-border-radius: 5; -fx-border-width: 1;";

    // מערך דו-ממדי ששומר את כל המשבצות (התאים) של הלו"ז. VBox הוא קונטיינר שמסדר אלמנטים אנכית.
    private VBox[][] gridCells = new VBox[DAYS_COUNT][HOURS_COUNT];

    // אובייקט לגישה למסד הנתונים
    private DatabaseRepository repository = new DatabaseRepository();

    // רשימות נגללות (ComboBox) שיעודכנו אוטומטית בעת רענון הלוח (כדי שיציגו נתונים עדכניים מה-DB)
    private ComboBox<Trainee> deleteTraineeCombo = new ComboBox<>();
    private ComboBox<Trainer> deleteTrainerCombo = new ComboBox<>();
    private ComboBox<Trainer> updateTrainerCombo = new ComboBox<>();

    /**
     * פונקציית ה-start היא נקודת ההתחלה של JavaFX. היא בונה את החלון הראשי.
     */
    @Override
    public void start(Stage primaryStage) {
        // BorderPane הוא פאנל שמחלק את המסך ל-5 אזורים (למעלה, למטה, ימין, שמאל, מרכז)
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10)); // שוליים של 10 פיקסלים סביב הכל
        root.setStyle("-fx-base-direction: rtl;"); // תמיכה בימין-לשמאל (עברית)

        // יצירת הכותרת הראשית והצבתה בחלק העליון (Top)
        Label mainTitle = new Label("מערכת שיבוץ אימונים חכמה");
        mainTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 10; -fx-text-fill: #2c3e50;");
        BorderPane.setAlignment(mainTitle, Pos.CENTER);
        root.setTop(mainTitle);

        // --- יצירת החלקים השונים של המסך ---
        GridPane scheduleGrid = createScheduleGrid(); // הלוח המרכזי
        VBox rightPanel = createTraineeManagementPanel(); // פאנל ימני - לניהול מתאמנים
        VBox leftPanel = createTrainerManagementPanel();  // פאנל שמאלי - לניהול מאמנים

        // ScrollPane מאפשר גלילה במקרה שהמסך קטן והלוח תופס הרבה מקום
        ScrollPane scrollPane = new ScrollPane(scheduleGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        // שיבוץ הפאנלים בתוך ה-BorderPane הראשי
        root.setCenter(scrollPane);
        root.setRight(rightPanel);
        root.setLeft(leftPanel);

        // קריאה ראשונה לרענון הלוח (שליפה מה-DB והרצת האלגוריתם)
        refreshSchedule();

        // הגדרת הסצנה (התוכן) ומידות החלון
        Scene scene = new Scene(root, 1350, 800);
        primaryStage.setTitle("Gym Scheduler 2026");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true); // פתיחת החלון על מסך מלא
        primaryStage.show();
    }

    // ==========================================
    // בניית לוח השעות (הגריד המרכזי)
    // ==========================================
    private GridPane createScheduleGrid() {
        // GridPane הוא פאנל שמסדר אלמנטים בצורת טבלה (שורות ועמודות)
        GridPane scheduleGrid = new GridPane();
        scheduleGrid.setStyle("-fx-border-color: #2c3e50; -fx-border-width: 2;");

        // הגדרת עמודות - חלוקה שווה של הרוחב בין עמודת השעות ו-7 ימי השבוע (סה"כ 8)
        for (int i = 0; i < 8; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 8);
            column.setHgrow(Priority.ALWAYS);
            scheduleGrid.getColumnConstraints().add(column);
        }

        // הגדרת השורות (שעות הפעילות + שורת כותרת לימים)
        for (int i = 0; i < HOURS_COUNT + 1; i++) {
            RowConstraints row = new RowConstraints();
            row.setVgrow(Priority.ALWAYS);
            scheduleGrid.getRowConstraints().add(row);
        }

        // המשבצת הריקה בפינה הימנית העליונה
        Label emptyCorner = new Label("שעה / יום");
        emptyCorner.setStyle(HEADER_STYLE);
        emptyCorner.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scheduleGrid.add(emptyCorner, 0, 0);

        // ציור שורת הכותרת העליונה (שמות הימים)
        for (int i = 0; i < DAYS_COUNT; i++) {
            Label dayLabel = new Label(DAY_NAMES[i]);
            dayLabel.setStyle(HEADER_STYLE);
            dayLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            scheduleGrid.add(dayLabel, i + 1, 0);
        }

        // יצירת המשבצות עבור כל שעה ביום
        for (int h = 0; h < HOURS_COUNT; h++) {
            // תווית השעה (למשל 08:00) בעמודה השמאלית ביותר
            Label hourLabel = new Label(String.format("%02d:00", h + 8));
            hourLabel.setStyle(HOUR_COLUMN_STYLE);
            hourLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            scheduleGrid.add(hourLabel, 0, h + 1);

            // יצירת המשבצות הריקות לאותה שעה לכל הימים
            for (int d = 0; d < DAYS_COUNT; d++) {
                VBox cell = new VBox(3);
                cell.setPadding(new Insets(2));
                cell.setStyle(CELL_STYLE);
                // אפקט "הובר" (שינוי צבע עדין כשהעכבר עובר על המשבצת)
                cell.setOnMouseEntered(e -> cell.setStyle(CELL_STYLE + "-fx-background-color: #f1f2f6;"));
                cell.setOnMouseExited(e -> cell.setStyle(CELL_STYLE));

                gridCells[d][h] = cell; // שמירה במערך הדו-ממדי שלנו לגישה עתידית
                scheduleGrid.add(cell, d + 1, h + 1);
            }
        }
        return scheduleGrid;
    }

    // ==========================================
    // בניית פאנל ניהול המתאמנים (צד ימין)
    // ==========================================
    private VBox createTraineeManagementPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(250);
        panel.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ced4da; -fx-border-width: 0 0 0 1;");

        Label title = new Label("ניהול מתאמנים");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // --- אזור הוספת מתאמן ---
        VBox addSection = new VBox(10);
        addSection.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 5;");
        Label addTitle = new Label("הוספת מתאמן חדש");
        addTitle.setStyle("-fx-font-weight: bold;");

        // תיבת טקסט ורשימות נפתחות לבחירת מאפייני המתאמן
        TextField nameInput = new TextField(); nameInput.setPromptText("שם המתאמן");
        ComboBox<TrainingGoal> goalCombo = new ComboBox<>(); goalCombo.getItems().addAll(TrainingGoal.values()); goalCombo.setPromptText("בחר מטרה"); goalCombo.setMaxWidth(Double.MAX_VALUE);
        ComboBox<FitnessLevel> levelCombo = new ComboBox<>(); levelCombo.getItems().addAll(FitnessLevel.values()); levelCombo.setPromptText("בחר רמה"); levelCombo.setMaxWidth(Double.MAX_VALUE);
        Spinner<Integer> workoutsSpinner = new Spinner<>(1, 7, 3); workoutsSpinner.setMaxWidth(Double.MAX_VALUE); // בחירת מספר אימונים עם חצים
        Label spinnerLabel = new Label("אימונים בשבוע:");
        Button addButton = new Button("הוסף מתאמן"); addButton.setMaxWidth(Double.MAX_VALUE); addButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");

        // אירוע הלחיצה על כפתור "הוסף מתאמן"
        addButton.setOnAction(e -> {
            // אימות שהשדות אינם ריקים
            if (nameInput.getText().isEmpty() || goalCombo.getValue() == null || levelCombo.getValue() == null) {
                showAlert("שגיאה", "נא למלא את כל השדות", Alert.AlertType.ERROR); return;
            }
            // קריאה לשמירה במסד הנתונים
            if (repository.addTrainee(nameInput.getText().trim(), levelCombo.getValue(), goalCombo.getValue(), workoutsSpinner.getValue())) {
                showAlert("הצלחה", "המתאמן נוסף בהצלחה!", Alert.AlertType.INFORMATION);
                nameInput.clear(); goalCombo.getSelectionModel().clearSelection(); levelCombo.getSelectionModel().clearSelection();
                refreshSchedule(); // רענון הלוח כדי להציג את המתאמן החדש
            } else {
                showAlert("שגיאה", "שגיאה בשמירה ל-DB", Alert.AlertType.ERROR);
            }
        });
        addSection.getChildren().addAll(addTitle, nameInput, goalCombo, levelCombo, spinnerLabel, workoutsSpinner, addButton);

        // --- אזור מחיקת מתאמן ---
        VBox deleteSection = new VBox(10);
        deleteSection.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 5;");
        Label deleteTitle = new Label("מחיקת מתאמן"); deleteTitle.setStyle("-fx-font-weight: bold;");
        deleteTraineeCombo.setMaxWidth(Double.MAX_VALUE); deleteTraineeCombo.setPromptText("בחר מתאמן");
        Button deleteButton = new Button("מחק מתאמן"); deleteButton.setMaxWidth(Double.MAX_VALUE); deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        // אירוע הלחיצה על מחיקה
        deleteButton.setOnAction(e -> {
            Trainee selected = deleteTraineeCombo.getValue();
            if (selected == null) { showAlert("שגיאה", "נא לבחור מתאמן", Alert.AlertType.WARNING); return; }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("אישור מחיקה");
            confirm.setHeaderText("האם אתה בטוח שברצונך למחוק את " + selected.getName() + "?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) { // אם המשתמש אישר את המחיקה
                    if (repository.deleteTrainee(selected.getId())) {
                        showAlert("הצלחה", "המתאמן נמחק בהצלחה!", Alert.AlertType.INFORMATION);
                        refreshSchedule();
                    } else {
                        showAlert("שגיאה", "שגיאה במחיקה מה-DB", Alert.AlertType.ERROR);
                    }
                }
            });
        });
        deleteSection.getChildren().addAll(deleteTitle, deleteTraineeCombo, deleteButton);

        panel.getChildren().addAll(title, addSection, deleteSection);
        return panel;
    }

    // ==========================================
    // בניית פאנל ניהול המאמנים (צד שמאל)
    // ==========================================
    private VBox createTrainerManagementPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(260);
        panel.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ced4da; -fx-border-width: 0 1 0 0;"); // קו הפרדה מימין לפאנל השמאלי

        Label title = new Label("צוות הדרכה");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // --- אזור הוספת מאמן ---
        VBox addSection = new VBox(10);
        addSection.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 5;");
        Label addTitle = new Label("הוספת מאמן חדש"); addTitle.setStyle("-fx-font-weight: bold;");

        TextField nameInput = new TextField(); nameInput.setPromptText("שם המאמן");

        Label specLabel = new Label("התמחויות (בחירה מרובה בעזרת Ctrl):");
        // ListView היא רשימה שמציגה את כל הפריטים (להבדיל מ-ComboBox שנפתחת).
        // הגדרת MULTIPLE מאפשרת בחירה של כמה שרירים יחד על ידי לחיצה על מקש Ctrl.
        ListView<MuscleGroup> specialtiesList = new ListView<>();
        specialtiesList.getItems().addAll(MuscleGroup.values());
        specialtiesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        specialtiesList.setPrefHeight(120);

        Button addButton = new Button("הוסף מאמן");
        addButton.setMaxWidth(Double.MAX_VALUE); addButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");

        addButton.setOnAction(e -> {
            if (nameInput.getText().isEmpty() || specialtiesList.getSelectionModel().getSelectedItems().isEmpty()) {
                showAlert("שגיאה", "נא להזין שם ולבחור לפחות התמחות אחת", Alert.AlertType.ERROR); return;
            }
            // איסוף כל ההתמחויות שהמשתמש סימן
            List<MuscleGroup> selectedSpecs = new ArrayList<>(specialtiesList.getSelectionModel().getSelectedItems());

            if(repository.addTrainer(nameInput.getText().trim(), selectedSpecs)) {
                showAlert("הצלחה", "המאמן נוסף בהצלחה! (יום החופש נקבע כברירת מחדל לשבת)", Alert.AlertType.INFORMATION);
                nameInput.clear(); specialtiesList.getSelectionModel().clearSelection();
                refreshSchedule();
            } else {
                showAlert("שגיאה", "שגיאה בשמירת המאמן ב-DB", Alert.AlertType.ERROR);
            }
        });
        addSection.getChildren().addAll(addTitle, nameInput, specLabel, specialtiesList, addButton);

        // --- אזור עדכון יום חופשי ---
        VBox availabilitySection = new VBox(10);
        availabilitySection.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 5;");
        Label availTitle = new Label("הגדרת יום חופש"); availTitle.setStyle("-fx-font-weight: bold;");

        updateTrainerCombo.setMaxWidth(Double.MAX_VALUE); updateTrainerCombo.setPromptText("בחר מאמן");

        ComboBox<String> dayOffCombo = new ComboBox<>();
        dayOffCombo.getItems().addAll(DAY_NAMES);
        dayOffCombo.setPromptText("בחר יום חופשי");
        dayOffCombo.setMaxWidth(Double.MAX_VALUE);

        Button updateBtn = new Button("עדכן יום חופשי");
        updateBtn.setMaxWidth(Double.MAX_VALUE); updateBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        updateBtn.setOnAction(e -> {
            Trainer selected = updateTrainerCombo.getValue();
            if(selected == null || dayOffCombo.getValue() == null){
                showAlert("שגיאה", "נא לבחור מאמן ויום חופשי", Alert.AlertType.WARNING); return;
            }
            // מקבל את המיקום ברשימה (0 עבור ראשון, 6 עבור שבת) שמתאים ללוגיקה שלנו
            int dayIndex = dayOffCombo.getSelectionModel().getSelectedIndex();

            if(repository.updateTrainerDayOff(selected.getId(), dayIndex)) {
                showAlert("הצלחה", "יום החופש עודכן בהצלחה!", Alert.AlertType.INFORMATION);
                refreshSchedule();
            } else {
                showAlert("שגיאה", "שגיאה בעדכון מסד הנתונים", Alert.AlertType.ERROR);
            }
        });
        availabilitySection.getChildren().addAll(availTitle, updateTrainerCombo, dayOffCombo, updateBtn);

        // --- אזור מחיקת מאמן ---
        VBox deleteSection = new VBox(10);
        deleteSection.setStyle("-fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 5;");
        Label deleteTitle = new Label("מחיקת מאמן"); deleteTitle.setStyle("-fx-font-weight: bold;");
        deleteTrainerCombo.setMaxWidth(Double.MAX_VALUE); deleteTrainerCombo.setPromptText("בחר מאמן למחיקה");
        Button deleteButton = new Button("מחק מאמן"); deleteButton.setMaxWidth(Double.MAX_VALUE); deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

        deleteButton.setOnAction(e -> {
            Trainer selected = deleteTrainerCombo.getValue();
            if (selected == null) { showAlert("שגיאה", "נא לבחור מאמן", Alert.AlertType.WARNING); return; }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("אישור מחיקה"); confirm.setHeaderText("האם למחוק את " + selected.getName() + "?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (repository.deleteTrainer(selected.getId())) {
                        showAlert("הצלחה", "המאמן נמחק בהצלחה!", Alert.AlertType.INFORMATION);
                        refreshSchedule();
                    } else {
                        showAlert("שגיאה", "שגיאה במחיקת המאמן", Alert.AlertType.ERROR);
                    }
                }
            });
        });
        deleteSection.getChildren().addAll(deleteTitle, deleteTrainerCombo, deleteButton);

        panel.getChildren().addAll(title, addSection, availabilitySection, deleteSection);
        return panel;
    }

    // ==========================================
    // לוגיקת ריצה ורענון הלוח המרכזי
    // ==========================================
    private void refreshSchedule() {
        // 1. ניקוי כל המשבצות בלוח מהרצת האלגוריתם הקודמת
        for (int d = 0; d < DAYS_COUNT; d++) {
            for (int h = 0; h < HOURS_COUNT; h++) {
                gridCells[d][h].getChildren().clear();
            }
        }

        // 2. שליפת כל הנתונים העדכניים ממסד הנתונים
        List<Workout> dbWorkouts = repository.getAllWorkouts();
        List<Trainer> dbTrainers = repository.getAllTrainers();
        List<Trainee> dbTrainees = repository.getAllTrainees();

        // 3. עדכון הרשימות הנפתחות (ComboBox) כדי שיציגו את השמות המעודכנים למחיקה/עדכון
        deleteTraineeCombo.getItems().setAll(dbTrainees);
        deleteTrainerCombo.getItems().setAll(dbTrainers);
        updateTrainerCombo.getItems().setAll(dbTrainers);

        // 4. אתחול האלגוריתם והרצתו על בסיס הנתונים החדשים
        ScheduleSolver solver = new ScheduleSolver(dbTrainers, dbWorkouts);
        List<Assignment> finalSchedule = solver.solve(dbTrainees);

        // 5. ציור הכרטיסיות החדשות של האימונים בתוך הלוח
        for (Assignment assignment : finalSchedule) {
            addAssignmentToGrid(assignment);
        }
    }

    /**
     * פונקציה לייצור כרטיסיית אימון (המלבן הכחול) ומיקומה בתוך המשבצת הנכונה בגריד.
     */
    private void addAssignmentToGrid(Assignment assignment) {
        int day = assignment.getDay();
        int hour = assignment.getHour() - 8; // תרגום השעה לאינדקס (למשל 08:00 הופך לאינדקס 0)

        // יצירת הכרטיסייה (VBox שמרכז בתוכו את הטקסטים)
        VBox card = new VBox(3);
        card.setStyle(ASSIGNMENT_CARD_STYLE);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(4));

        Label workoutLbl = new Label(assignment.getWorkout().getName());
        workoutLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436; -fx-font-size: 12px;");
        workoutLbl.setWrapText(true); // שבירת שורה במידה והטקסט ארוך
        workoutLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label traineeLbl = new Label("מתאמן: " + assignment.getTrainee().getName());
        traineeLbl.setStyle("-fx-text-fill: #0984e3; -fx-font-weight: bold; -fx-font-size: 11px;");

        Label trainerLbl = new Label("מאמן: " + assignment.getTrainer().getName());
        trainerLbl.setStyle("-fx-text-fill: #2d3436; -fx-font-size: 11px;");

        card.getChildren().addAll(workoutLbl, traineeLbl, trainerLbl);

        // Tooltip: חלונית מידע שקופצת כשמרחפים עם העכבר מעל הכרטיסייה
        Tooltip tooltip = new Tooltip(
                "סוג אימון: " + assignment.getWorkout().getName() + "\n" +
                        "מתאמן: " + assignment.getTrainee().getName() + "\n" +
                        "צוות הדרכה: " + assignment.getTrainer().getName()
        );
        tooltip.setStyle("-fx-font-size: 14px; -fx-text-alignment: right;");
        Tooltip.install(card, tooltip);

        // הוספת הכרטיסייה למשבצת הרלוונטית אם היא בטווח התקין
        if(day >= 0 && day < DAYS_COUNT && hour >= 0 && hour < HOURS_COUNT) {
            gridCells[day][hour].getChildren().add(card);
        }
    }

    /**
     * פונקציית עזר להקפצת חלונות הודעה (הצלחה/שגיאה) למשתמש
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args); // הפעלת האפליקציה (קריאה ל-start מאחורי הקלעים)
    }
}