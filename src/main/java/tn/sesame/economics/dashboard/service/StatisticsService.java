package tn.sesame.economics.dashboard.service;

import tn.sesame.economics.dashboard.model.DashboardStatistics;
import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ProductType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating dashboard statistics
 */
public class StatisticsService {

    public DashboardStatistics calculateStatistics(List<PricePrediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return createEmptyStatistics();
        }

        // Basic price statistics
        double[] prices = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .toArray();

        double avgPrice = Arrays.stream(prices).average().orElse(0.0);
        double minPrice = Arrays.stream(prices).min().orElse(0.0);
        double maxPrice = Arrays.stream(prices).max().orElse(0.0);
        double variance = Arrays.stream(prices)
                .map(p -> Math.pow(p - avgPrice, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        // Confidence statistics
        double avgConfidence = predictions.stream()
                .mapToDouble(PricePrediction::confidence)
                .average()
                .orElse(0.0);

        long highConfidence = predictions.stream()
                .filter(p -> p.confidence() >= 0.8)
                .count();

        long mediumConfidence = predictions.stream()
                .filter(p -> p.confidence() >= 0.6 && p.confidence() < 0.8)
                .count();

        long lowConfidence = predictions.stream()
                .filter(p -> p.confidence() < 0.6)
                .count();

        // Product distribution
        Map<String, Double> productDistribution = predictions.stream()
                .collect(Collectors.groupingBy(
                        p -> p.productType().getFrenchName(),
                        Collectors.averagingDouble(PricePrediction::predictedPrice)
                ));

        // For demonstration, create country distribution
        Map<String, Integer> countryDistribution = new HashMap<>();
        countryDistribution.put("France", 35);
        countryDistribution.put("Germany", 28);
        countryDistribution.put("Italy", 22);
        countryDistribution.put("Spain", 15);
        countryDistribution.put("UK", 10);

        return DashboardStatistics.builder()
                .averagePrice(avgPrice)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .standardDeviation(stdDev)
                .productDistribution(productDistribution)
                .countryDistribution(countryDistribution)
                .totalPredictions(predictions.size())
                .averageConfidence(avgConfidence)
                .highConfidenceCount((int) highConfidence)
                .mediumConfidenceCount((int) mediumConfidence)
                .lowConfidenceCount((int) lowConfidence)
                .build();
    }
    // Add this method to your StatisticsService class:
    public DashboardStatistics calculateStatisticsFromRealData(DataService dataService) {
        List<PricePrediction> predictions = dataService.generateRealPredictions();
        java.util.Map<String, Double> productDistribution = dataService.getProductPriceDistribution();
        java.util.Map<String, Integer> countryDistribution = dataService.getCountryDistribution();

        return calculateStatistics(predictions, productDistribution, countryDistribution);
    }

    public DashboardStatistics calculateStatistics(List<PricePrediction> predictions,
                                                   java.util.Map<String, Double> productDistribution,
                                                   java.util.Map<String, Integer> countryDistribution) {
        DashboardStatistics stats = calculateStatistics(predictions);

        // Use provided distributions or create from predictions
        if (productDistribution != null && !productDistribution.isEmpty()) {
            stats.setProductDistribution(productDistribution);
        } else {
            // Calculate from predictions if no distribution provided
            stats.setProductDistribution(calculateProductDistribution(predictions));
        }

        if (countryDistribution != null && !countryDistribution.isEmpty()) {
            stats.setCountryDistribution(countryDistribution);
        }

        return stats;
    }

    private DashboardStatistics createEmptyStatistics() {
        return DashboardStatistics.builder()
                .averagePrice(0.0)
                .minPrice(0.0)
                .maxPrice(0.0)
                .standardDeviation(0.0)
                .productDistribution(new HashMap<>())
                .countryDistribution(new HashMap<>())
                .totalPredictions(0)
                .averageConfidence(0.0)
                .highConfidenceCount(0)
                .mediumConfidenceCount(0)
                .lowConfidenceCount(0)
                .build();
    }
    private java.util.Map<String, Double> calculateProductDistribution(List<PricePrediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return new java.util.HashMap<>();
        }

        return predictions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        p -> p.productType().getFrenchName(),
                        java.util.stream.Collectors.averagingDouble(PricePrediction::predictedPrice)
                ));
    }
}