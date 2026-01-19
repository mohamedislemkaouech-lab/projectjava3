package tn.sesame.economics.dashboard.util.chart;

import javafx.scene.canvas.GraphicsContext;
import java.util.Map;

/**
 * Strategy Pattern: Interface for different chart types
 */
@FunctionalInterface
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
                "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
                "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
        };
        return colors[index % colors.length];
    }
}