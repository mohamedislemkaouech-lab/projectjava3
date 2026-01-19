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
            DashboardController controller = new DashboardController(
                    primaryStage,
                    intelligenceService != null ? intelligenceService : createDummyService()
            );
            controller.initialize(); // ADD THIS LINE
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start dashboard: " + e.getMessage());
        }
    }

    private EconomicIntelligenceService createDummyService() {
        // Create a dummy service for testing
        return null; // You'll replace this with actual service
    }

    public static void main(String[] args) {
        launch(args);
    }
}