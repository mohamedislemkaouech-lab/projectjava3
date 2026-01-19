package tn.sesame.economics.dashboard.view;

import tn.sesame.economics.dashboard.model.DashboardStatistics;
import tn.sesame.economics.dashboard.service.DataService;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Panel for displaying time trends
 */
public class TimeTrendsPanel extends VBox {

    private final DataService dataService;
    private Label monthlyTrendLabel;
    private Label quarterlyTrendLabel;
    private Label seasonalPatternLabel;

    public TimeTrendsPanel(DataService dataService) { // Make sure this constructor exists
        this.dataService = dataService;
        initializeUI();
    }

    private void initializeUI() {
        setPadding(new Insets(20));
        setSpacing(15);
        setStyle("-fx-background-color: #fff0f5; -fx-border-color: #d8bfd8; -fx-border-radius: 5;");

        Label title = new Label("📅 TIME TRENDS & SEASONALITY");
        title.setFont(Font.font("Arial", 16));
        title.setTextFill(Color.DARKBLUE);

        monthlyTrendLabel = createTrendLabel("Analyzing monthly trends...");
        quarterlyTrendLabel = createTrendLabel("Calculating quarterly patterns...");
        seasonalPatternLabel = createTrendLabel("Identifying seasonal patterns...");

        getChildren().addAll(title, monthlyTrendLabel, quarterlyTrendLabel, seasonalPatternLabel);
    }

    private Label createTrendLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", 12));
        label.setTextFill(Color.DARKSLATEGRAY);
        label.setWrapText(true);
        return label;
    }

    public void updateTrends() {
        // For now, show placeholder trends
        // In a real implementation, you would analyze historical data

        monthlyTrendLabel.setText("📈 Monthly Trend: Peak exports in November (Olive Oil harvest)");
        quarterlyTrendLabel.setText("📊 Q4 shows 25% higher prices than Q1 average");
        seasonalPatternLabel.setText("🌿 Strong seasonality: +15% price premium in winter months");

        // You can later implement real trend analysis here
        // analyzeHistoricalTrends(dataService.loadHistoricalData());
    }
}