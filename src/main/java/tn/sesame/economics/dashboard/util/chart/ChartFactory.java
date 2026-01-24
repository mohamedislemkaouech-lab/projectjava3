package tn.sesame.economics.dashboard.util.chart;

import java.util.Arrays;

/**
 * Factory Pattern: Creates different chart strategies
 */
public class ChartFactory {

    public enum ChartType {
        BAR, LINE, PIE
    }

    /**
     * Factory method to create chart strategies
     */
    public static ChartStrategy createChart(ChartType type) {
        return switch (type) {
            case BAR -> new BarChartStrategy();
            case LINE -> new LineChartStrategy();
            case PIE -> new PieChartStrategy();
            default -> throw new IllegalArgumentException("Unknown chart type: " + type);
        };
    }

    /**
     * Create chart by name
     */
    public static ChartStrategy createChart(String typeName) {
        System.out.println("🔧 ChartFactory.createChart() called with: " + typeName);
        try {
            ChartType type = ChartType.valueOf(typeName.toUpperCase().trim());
            System.out.println("🔧 Creating chart of type: " + type);
            return createChart(type);
        } catch (IllegalArgumentException e) {
            System.err.println("❌ ERROR: Unknown chart type: " + typeName);
            System.err.println("Available types: " + Arrays.toString(ChartType.values()));
            throw new IllegalArgumentException("Unknown chart type: " + typeName +
                    ". Available types: BAR, LINE, PIE", e);
        }
    }

    /**
     * Get all available chart types
     */
    public static String[] getAvailableChartTypes() {
        ChartType[] types = ChartType.values();
        String[] typeNames = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].name();
        }
        return typeNames;
    }

    /**
     * Get description for each chart type
     */
    public static String getChartDescription(ChartType type) {
        return switch (type) {
            case BAR -> "Bar Chart - Compares values across categories";
            case LINE -> "Line Chart - Shows trends over time or sequence";
            case PIE -> "Pie Chart - Shows proportions and percentages";
        };
    }
}