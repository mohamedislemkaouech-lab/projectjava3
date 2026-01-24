package tn.sesame.economics.dashboard.util.chart;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Concrete Strategy: Line Chart implementation for time series
 */
public class LineChartStrategy implements ChartStrategy {

    @Override
    public void drawChart(GraphicsContext gc, Map<String, Double> data,
                          double width, double height, String title) {

        System.out.println("🔧 LineChartStrategy.drawChart() called");

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

        // Prepare data
        List<Map.Entry<String, Double>> entries = new ArrayList<>(data.entrySet());

        // Find min and max values for scaling
        double maxValue = entries.stream()
                .mapToDouble(Map.Entry::getValue)
                .max()
                .orElse(1.0);
        double minValue = entries.stream()
                .mapToDouble(Map.Entry::getValue)
                .min()
                .orElse(0.0);
        double valueRange = maxValue - minValue;
        if (valueRange == 0) valueRange = 1;

        System.out.println("  - Min value: " + minValue + ", Max value: " + maxValue);

        // Draw grid lines
        drawGridLines(gc, chartX, chartY, chartWidth, chartHeight, minValue, maxValue);

        // Draw line
        gc.setStroke(Color.web("#1E88E5")); // Blue color
        gc.setLineWidth(3);

        for (int i = 0; i < entries.size() - 1; i++) {
            Map.Entry<String, Double> current = entries.get(i);
            Map.Entry<String, Double> next = entries.get(i + 1);

            double x1 = chartX + (i * chartWidth / (entries.size() - 1));
            double y1 = chartY + chartHeight - ((current.getValue() - minValue) / valueRange * chartHeight);

            double x2 = chartX + ((i + 1) * chartWidth / (entries.size() - 1));
            double y2 = chartY + chartHeight - ((next.getValue() - minValue) / valueRange * chartHeight);

            gc.strokeLine(x1, y1, x2, y2);

            // Draw data point
            drawDataPoint(gc, x1, y1, Color.web("#E53935"), 5); // Red points
        }

        // Draw last data point
        if (!entries.isEmpty()) {
            Map.Entry<String, Double> last = entries.get(entries.size() - 1);
            double x = chartX + chartWidth;
            double y = chartY + chartHeight - ((last.getValue() - minValue) / valueRange * chartHeight);
            drawDataPoint(gc, x, y, Color.web("#E53935"), 5);
        }

        // Draw labels
        drawLabels(gc, entries, chartX, chartY, chartWidth, chartHeight);

        System.out.println("✅ Line chart drawn successfully");
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
    }

    private void drawGridLines(GraphicsContext gc, double x, double y, double width,
                               double height, double minValue, double maxValue) {
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.5);

        // Horizontal grid lines
        int horizontalLines = 5;
        for (int i = 0; i <= horizontalLines; i++) {
            double lineY = y + height - (i * height / horizontalLines);
            gc.strokeLine(x, lineY, x + width, lineY);

            // Y-axis labels
            double value = minValue + (i * (maxValue - minValue) / horizontalLines);
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", 10));
            gc.fillText(String.format("%.1f", value), x - 35, lineY + 4);
        }
    }

    private void drawDataPoint(GraphicsContext gc, double x, double y, Color color, double radius) {
        gc.setFill(color);
        gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    private void drawLabels(GraphicsContext gc, List<Map.Entry<String, Double>> entries,
                            double x, double y, double width, double height) {
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", 10));

        for (int i = 0; i < entries.size(); i++) {
            if (i % Math.max(1, entries.size() / 5) == 0 || i == entries.size() - 1) {
                double labelX = x + (i * width / (entries.size() - 1));
                String label = entries.get(i).getKey();

                // Rotate text for better fit
                gc.save();
                gc.translate(labelX, y + height + 20);
                gc.rotate(45);
                gc.fillText(label, 0, 0);
                gc.restore();

                // Draw tick mark
                gc.setStroke(Color.BLACK);
                gc.setLineWidth(1);
                gc.strokeLine(labelX, y + height, labelX, y + height + 5);
            }
        }
    }

    private void drawNoDataMessage(GraphicsContext gc, double width, double height) {
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, width, height);

        gc.setFill(Color.DARKGRAY);
        gc.setFont(Font.font("Arial", 14));
        String message = "No data available for line chart";
        gc.fillText(message, width / 2 - (message.length() * 3.5), height / 2);
    }
}