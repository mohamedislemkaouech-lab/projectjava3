package tn.sesame.economics.dashboard.view;

import tn.sesame.economics.dashboard.model.DashboardStatistics;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight; // ADD THIS IMPORT
import javafx.scene.shape.Rectangle; // ADD THIS IMPORT
import java.util.Map;

/**
 * Panel for displaying product distribution
 */
public class ProductDistributionPanel extends VBox {

    public ProductDistributionPanel() {
        initializeUI();
    }

    private void initializeUI() {
        setPadding(new Insets(20));
        setSpacing(10);
        setStyle("-fx-background-color: #f0f8ff; -fx-border-color: #b0c4de; -fx-border-radius: 5;");

        Label title = new Label("📦 PRODUCT DISTRIBUTION");
        title.setFont(Font.font("Arial", 16));
        title.setTextFill(Color.DARKBLUE);

        getChildren().add(title);
    }

    public void updateProductDistribution(Map<String, Double> distribution) {
        // Clear previous content except title
        if (getChildren().size() > 1) {
            getChildren().remove(1, getChildren().size());
        }

        if (distribution.isEmpty()) {
            Label noData = new Label("No product data available");
            noData.setTextFill(Color.GRAY);
            getChildren().add(noData);
            return;
        }

        distribution.forEach((product, avgPrice) -> {
            HBox row = new HBox(10);
            row.setPadding(new Insets(5, 0, 5, 0));

            Label productLabel = new Label(product);
            productLabel.setMinWidth(150);
            productLabel.setFont(Font.font("Arial", 14));

            // FIXED: Use Font.font(String, FontWeight, double) signature
            Label priceLabel = new Label(String.format("%.2f TND", avgPrice));
            priceLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14)); // Changed here
            priceLabel.setTextFill(Color.DARKGREEN);

            // Simple progress bar visualization
            double barWidth = Math.min(avgPrice / 1000.0 * 100, 200); // Scale for visualization
            Rectangle bar = new Rectangle(barWidth, 10); // Use Rectangle directly
            bar.setFill(Color.LIGHTBLUE);
            bar.setStroke(Color.DARKBLUE);

            row.getChildren().addAll(productLabel, priceLabel, bar);
            getChildren().add(row);
        });
    }
}