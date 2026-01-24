package tn.sesame.economics.dashboard.util.chart;

import javafx.scene.canvas.GraphicsContext;
import java.util.Map;

/**
 * Strategy Pattern: Interface for different chart types
 */
public interface ChartStrategy {

    /**
     * Draw a chart on the canvas
     * @param gc GraphicsContext for drawing
     * @param data Map of data points (label -> value)
     * @param width Canvas width
     * @param height Canvas height
     * @param title Chart title
     */
    void drawChart(GraphicsContext gc, Map<String, Double> data,
                   double width, double height, String title);

    /**
     * Default method for chart validation
     */
    default boolean validateData(Map<String, Double> data) {
        return data != null && !data.isEmpty();
    }

    /**
     * Default method to calculate chart colors
     */
    default String getColorForIndex(int index) {
        String[] colors = {
                "#1E88E5", // Blue
                "#43A047", // Green
                "#FF6F00", // Orange
                "#8E24AA", // Purple
                "#E53935", // Red
                "#00ACC1", // Cyan
                "#FFA726", // Yellow
                "#5C6BC0", // Indigo
                "#26A69A", // Teal
                "#7CB342"  // Light Green
        };
        return colors[index % colors.length];
    }
}