package tn.sesame.economics;

import tn.sesame.economics.dashboard.controller.DashboardController;
import tn.sesame.economics.service.EconomicIntelligenceService;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX application launcher for the dashboard
 */
public class DashboardLauncher extends Application {

    private static EconomicIntelligenceService intelligenceService;

    public static void setIntelligenceService(EconomicIntelligenceService service) {
        intelligenceService = service;
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Use the intelligence service passed from Main
            EconomicIntelligenceService service = intelligenceService;

            // If no service was set, we can't proceed
            if (service == null) {
                System.err.println("❌ Intelligence service not initialized");
                System.err.println("💡 The dashboard requires an active AI model");
                javafx.application.Platform.exit();
                return;
            }

            // Create and initialize the dashboard controller
            DashboardController controller = new DashboardController(primaryStage, service);

            // Initialize and show the dashboard
            controller.initialize();

            System.out.println("✅ Dashboard launched successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Failed to start dashboard: " + e.getMessage());

            // Show error dialog
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle("Dashboard Error");
            alert.setHeaderText("Failed to launch dashboard");
            alert.setContentText("Error: " + e.getMessage() + "\n\nCheck console for details.");
            alert.showAndWait();

            javafx.application.Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}