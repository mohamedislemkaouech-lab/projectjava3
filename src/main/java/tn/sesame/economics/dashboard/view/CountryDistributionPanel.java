package tn.sesame.economics.dashboard.view;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.shape.Rectangle;
import java.util.Map;

/**
 * Panel for displaying country distribution
 */
public class CountryDistributionPanel extends VBox {

    public CountryDistributionPanel() {
        initializeUI();
    }

    private void initializeUI() {
        setPadding(new Insets(20));
        setSpacing(10);
        setStyle("-fx-background-color: #f0fff0; -fx-border-color: #90ee90; -fx-border-radius: 5;");

        Label title = new Label("🌍 COUNTRY DISTRIBUTION");
        title.setFont(Font.font("Arial", 16));
        title.setTextFill(Color.DARKBLUE);

        getChildren().add(title);
    }

    public void updateCountryDistribution(Map<String, Integer> distribution) {
        // Clear previous content except title
        if (getChildren().size() > 1) {
            getChildren().remove(1, getChildren().size());
        }

        if (distribution.isEmpty()) {
            Label noData = new Label("No country data available");
            noData.setTextFill(Color.GRAY);
            getChildren().add(noData);
            return;
        }

        // Sort by count (descending)
        distribution.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(8) // Show top 8 countries
                .forEach(entry -> {
                    HBox row = new HBox(10);
                    row.setPadding(new Insets(5, 0, 5, 0));

                    Label countryLabel = new Label(entry.getKey());
                    countryLabel.setMinWidth(120);
                    countryLabel.setFont(Font.font("Arial", 14));

                    Label countLabel = new Label(entry.getValue() + " exports");
                    countLabel.setFont(Font.font("Arial", 14));
                    countLabel.setTextFill(Color.DARKGREEN);

                    // Progress bar based on count
                    int maxCount = distribution.values().stream().max(Integer::compare).orElse(1);
                    double barWidth = (entry.getValue() / (double) maxCount) * 150;
                    Rectangle bar = new Rectangle(barWidth, 10);
                    bar.setFill(Color.LIGHTGREEN);
                    bar.setStroke(Color.DARKGREEN);

                    row.getChildren().addAll(countryLabel, countLabel, bar);
                    getChildren().add(row);
                });
    }
}