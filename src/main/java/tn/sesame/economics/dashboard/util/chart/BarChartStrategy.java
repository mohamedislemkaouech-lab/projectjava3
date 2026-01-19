package tn.sesame.economics.dashboard.util.chart;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Concrete Strategy: Bar Chart implementation
 */
public class BarChartStrategy implements ChartStrategy {

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

        // Calculate chart area
        double chartX = 80;
        double chartY = 60;
        double chartWidth = width - 120;
        double chartHeight = height - 120;

        // Draw axes
        drawAxes(gc, chartX, chartY, chartWidth, chartHeight);

        // Prepare data for drawing
        List<Map.Entry<String, Double>> entries = new ArrayList<>(data.entrySet());

        // Find max value for scaling
        double maxValue = entries.stream()
                .mapToDouble(Map.Entry::getValue)
                .max()
                .orElse(1.0);

        // Calculate bar dimensions
        double barWidth = chartWidth / entries.size() * 0.7;
        double spacing = chartWidth / entries.size() * 0.3;

        // Draw bars
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Double> entry = entries.get(i);
            String label = entry.getKey();
            double value = entry.getValue();

            // Calculate bar position and height
            double barX = chartX + i * (barWidth + spacing);
            double barHeight = (value / maxValue) * chartHeight;
            double barY = chartY + chartHeight - barHeight;

            // Draw bar
            gc.setFill(Color.web(getColorForIndex(i)));
            gc.fillRect(barX, barY, barWidth, barHeight);

            // Draw bar border
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeRect(barX, barY, barWidth, barHeight);

            // Draw value on top of bar
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", 10));
            String valueText = String.format("%.1f", value);
            gc.fillText(valueText, barX + barWidth/2 - 10, barY - 5);

            // Draw label below bar
            drawRotatedText(gc, label, barX + barWidth/2, chartY + chartHeight + 15, 45);
        }

        // Draw legend
        drawLegend(gc, entries, width - 150, 80);
    }

    private void drawTitle(GraphicsContext gc, String title, double width) {
        gc.setFill(Color.DARKBLUE);
        gc.setFont(Font.font("Arial", 16));
        gc.fillText(title, width / 2 - (title.length() * 4), 30);
    }

    private void drawAxes(GraphicsContext gc, double x, double y, double width, double height) {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);

        // Y-axis
        gc.strokeLine(x, y, x, y + height);

        // X-axis
        gc.strokeLine(x, y + height, x + width, y + height);

        // Y-axis label
        gc.save();
        gc.translate(20, y + height / 2);
        gc.rotate(-90);
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 12));
        gc.fillText("Values", 0, 0);
        gc.restore();

        // X-axis label
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 12));
        gc.fillText("Categories", x + width / 2 - 30, y + height + 40);
    }

    private void drawRotatedText(GraphicsContext gc, String text, double x, double y, double angle) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(angle);
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText(text, 0, 0);
        gc.restore();
    }

    private void drawLegend(GraphicsContext gc, List<Map.Entry<String, Double>> entries,
                            double startX, double startY) {
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 12));
        gc.fillText("Legend:", startX, startY);

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Double> entry = entries.get(i);
            double y = startY + 25 + i * 20;

            // Draw color box
            gc.setFill(Color.web(getColorForIndex(i)));
            gc.fillRect(startX, y - 10, 15, 10);

            // Draw label
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", 10));
            gc.fillText(entry.getKey() + ": " + String.format("%.1f", entry.getValue()),
                    startX + 20, y);
        }
    }

    private void drawNoDataMessage(GraphicsContext gc, double width, double height) {
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, width, height);

        gc.setFill(Color.DARKGRAY);
        gc.setFont(Font.font("Arial", 14));
        String message = "No data available for chart";
        gc.fillText(message, width / 2 - (message.length() * 3.5), height / 2);
    }
}