package tn.sesame.economics.dashboard.view;

import tn.sesame.economics.dashboard.model.DashboardStatistics;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight; // ADD THIS IMPORT
import javafx.scene.paint.Color;

/**
 * Panel for displaying statistics
 */
public class StatisticsPanel extends VBox {

    private Label avgPriceLabel;
    private Label priceRangeLabel;
    private Label stdDevLabel;
    private Label totalPredictionsLabel;
    private Label avgConfidenceLabel;
    private Label confidenceDistributionLabel;

    public StatisticsPanel() {
        initializeUI();
    }

    private void initializeUI() {
        setPadding(new Insets(20));
        setSpacing(15);
        setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-radius: 5;");

        // Title
        Label title = new Label("📊 DASHBOARD STATISTICS");
        title.setFont(Font.font("Arial", 18));
        title.setTextFill(Color.DARKBLUE);

        // Create grid for statistics
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // Initialize labels
        avgPriceLabel = createStatLabel("0.00 TND");
        priceRangeLabel = createStatLabel("0.00 - 0.00 TND");
        stdDevLabel = createStatLabel("0.00 TND");
        totalPredictionsLabel = createStatLabel("0");
        avgConfidenceLabel = createStatLabel("0.00%");
        confidenceDistributionLabel = createStatLabel("High: 0 | Medium: 0 | Low: 0");

        // Add to grid
        grid.add(createDescriptionLabel("Average Price:"), 0, 0);
        grid.add(avgPriceLabel, 1, 0);

        grid.add(createDescriptionLabel("Price Range:"), 0, 1);
        grid.add(priceRangeLabel, 1, 1);

        grid.add(createDescriptionLabel("Standard Deviation:"), 0, 2);
        grid.add(stdDevLabel, 1, 2);

        grid.add(createDescriptionLabel("Total Predictions:"), 0, 3);
        grid.add(totalPredictionsLabel, 1, 3);

        grid.add(createDescriptionLabel("Average Confidence:"), 0, 4);
        grid.add(avgConfidenceLabel, 1, 4);

        grid.add(createDescriptionLabel("Confidence Distribution:"), 0, 5);
        grid.add(confidenceDistributionLabel, 1, 5);

        getChildren().addAll(title, grid);
    }

    private Label createDescriptionLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", 14));
        label.setTextFill(Color.DARKSLATEGRAY);
        label.setMinWidth(180);
        return label;
    }

    private Label createStatLabel(String text) {
        Label label = new Label(text);
        // FIXED: Use Font.font(String, FontWeight, double) signature
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14)); // Changed here
        label.setTextFill(Color.DARKGREEN);
        label.setStyle("-fx-background-color: white; -fx-padding: 5px; -fx-border-color: #ccc;");
        label.setMinWidth(200);
        return label;
    }

    public void updateStatistics(DashboardStatistics stats) {
        avgPriceLabel.setText(String.format("%.2f TND", stats.getAveragePrice()));
        priceRangeLabel.setText(stats.getPriceRange());
        stdDevLabel.setText(String.format("%.2f TND", stats.getStandardDeviation()));
        totalPredictionsLabel.setText(String.valueOf(stats.getTotalPredictions()));
        avgConfidenceLabel.setText(String.format("%.2f%%", stats.getConfidencePercentage()));

        String confidenceDist = String.format("High: %d | Medium: %d | Low: %d",
                stats.getHighConfidenceCount(),
                stats.getMediumConfidenceCount(),
                stats.getLowConfidenceCount());
        confidenceDistributionLabel.setText(confidenceDist);
    }
}