package tn.sesame.economics.dashboard.util.chart;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Concrete Strategy: Pie Chart implementation
 */
public class PieChartStrategy implements ChartStrategy {

    @Override
    public void drawChart(GraphicsContext gc, Map<String, Double> data,
                          double width, double height, String title) {

        if (!validateData(data)) {
            drawNoDataMessage(gc, width, height);
            return;
        }

        // Clear canvas
        gc.clearRect(0, 0, width, height);

        // Set background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);

        // Draw title
        drawTitle(gc, title, width);

        // Prepare data
        List<Map.Entry<String, Double>> entries = new ArrayList<>(data.entrySet());

        // Calculate total for percentages
        double total = entries.stream()
                .mapToDouble(Map.Entry::getValue)
                .sum();

        // Calculate pie chart position and size
        double centerX = width / 2;
        double centerY = height / 2 + 20;
        double radius = Math.min(width, height) / 3;

        // Draw pie slices
        double startAngle = 0;

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Double> entry = entries.get(i);
            double value = entry.getValue();
            double percentage = (value / total) * 100;
            double sliceAngle = (value / total) * 360;

            // Draw slice
            gc.setFill(Color.web(getColorForIndex(i)));
            gc.fillArc(centerX - radius, centerY - radius,
                    radius * 2, radius * 2,
                    startAngle, sliceAngle,
                    javafx.scene.shape.ArcType.ROUND);

            // Draw slice border
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeArc(centerX - radius, centerY - radius,
                    radius * 2, radius * 2,
                    startAngle, sliceAngle,
                    javafx.scene.shape.ArcType.ROUND);

            // Calculate label position (middle of slice)
            double midAngle = startAngle + sliceAngle / 2;
            double labelRadius = radius * 0.7;
            double labelX = centerX + labelRadius * Math.cos(Math.toRadians(midAngle - 90));
            double labelY = centerY + labelRadius * Math.sin(Math.toRadians(midAngle - 90));

            // Draw percentage label
            if (sliceAngle > 10) { // Only draw if slice is big enough
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("Arial", 10));
                String percentText = String.format("%.1f%%", percentage);
                gc.fillText(percentText, labelX - 10, labelY + 4);
            }

            startAngle += sliceAngle;
        }

        // Draw legend
        drawLegend(gc, entries, total, 50, height - 100);
    }

    private void drawTitle(GraphicsContext gc, String title, double width) {
        gc.setFill(Color.DARKBLUE);
        gc.setFont(Font.font("Arial", 16));
        gc.fillText(title, width / 2 - (title.length() * 4), 30);
    }

    private void drawLegend(GraphicsContext gc, List<Map.Entry<String, Double>> entries,
                            double total, double startX, double startY) {
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 12));
        gc.fillText("Distribution:", startX, startY);

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Double> entry = entries.get(i);
            double y = startY + 25 + i * 20;
            double percentage = (entry.getValue() / total) * 100;

            // Draw color box
            gc.setFill(Color.web(getColorForIndex(i)));
            gc.fillRect(startX, y - 10, 15, 10);

            // Draw label with percentage
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", 10));
            String label = String.format("%s: %.1f (%.1f%%)",
                    entry.getKey(), entry.getValue(), percentage);
            gc.fillText(label, startX + 20, y);
        }
    }

    private void drawNoDataMessage(GraphicsContext gc, double width, double height) {
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, width, height);

        gc.setFill(Color.DARKGRAY);
        gc.setFont(Font.font("Arial", 14));
        String message = "No data available for pie chart";
        gc.fillText(message, width / 2 - (message.length() * 3.5), height / 2);
    }
}