package tn.sesame.economics.dashboard.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * Model for dashboard statistics data
 */
@Data
@Builder
public class DashboardStatistics {
    private double averagePrice;
    private double minPrice;
    private double maxPrice;
    private double standardDeviation;
    private Map<String, Double> productDistribution;  // Product → Average Price
    private Map<String, Integer> countryDistribution; // Country → Export Count
    private int totalPredictions;
    private double averageConfidence;
    private int highConfidenceCount;
    private int mediumConfidenceCount;
    private int lowConfidenceCount;

    // Helper methods
    public String getPriceRange() {
        return String.format("%.2f - %.2f TND", minPrice, maxPrice);
    }

    public double getConfidencePercentage() {
        return averageConfidence * 100;
    }
}