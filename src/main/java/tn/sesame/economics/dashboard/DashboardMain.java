package tn.sesame.economics.dashboard;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Main JavaFX dashboard application
 */
public class DashboardMain extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tunisian Economic Intelligence Dashboard");

        BorderPane root = new BorderPane();
        root.setTop(createHeader());
        root.setCenter(createMainContent());
        root.setBottom(createFooter());

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Label createHeader() {
        Label header = new Label("🤖 Tunisian Agricultural Export Intelligence System");
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 20px;");
        return header;
    }

    private Label createMainContent() {
        return new Label("Dashboard content will be implemented here");
    }

    private Label createFooter() {
        Label footer = new Label("© 2026 SESAME University - Economic Intelligence System");
        footer.setStyle("-fx-padding: 10px; -fx-font-size: 12px;");
        return footer;
    }

    public static void main(String[] args) {
        launch(args);
    }
}