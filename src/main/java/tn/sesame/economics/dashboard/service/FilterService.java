package tn.sesame.economics.dashboard.service;

import tn.sesame.economics.model.ExportData;
import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ProductType;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Service for filtering data using functional interfaces (Predicates)
 * Demonstrates use of Java Stream API and functional programming
 */
public class FilterService {

    /**
     * Create a predicate for filtering by product type
     */
    public static Predicate<PricePrediction> createProductFilter(String productName) {
        if ("All Products".equals(productName) || productName == null) {
            return prediction -> true; // Return all
        }

        return prediction -> {
            String frenchName = prediction.productType().getFrenchName();
            return frenchName.equals(productName);
        };
    }

    /**
     * Create a predicate for filtering by country
     */
    public static Predicate<ExportData> createCountryFilter(String countryName) {
        if ("All Countries".equals(countryName) || countryName == null) {
            return data -> true; // Return all
        }

        return data -> data.destinationCountry().equalsIgnoreCase(countryName);
    }

    /**
     * Create a predicate for filtering by date range
     */
    public static Predicate<ExportData> createDateRangeFilter(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return data -> true; // Return all
        }

        return data -> {
            LocalDate exportDate = data.date();
            return !exportDate.isBefore(startDate) && !exportDate.isAfter(endDate);
        };
    }

    /**
     * Create a predicate for filtering predictions by date range
     */
    public static Predicate<PricePrediction> createPredictionDateFilter(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return prediction -> true; // Return all
        }

        return prediction -> {
            LocalDate predictionDate = prediction.predictionDate();
            return !predictionDate.isBefore(startDate) && !predictionDate.isAfter(endDate);
        };
    }

    /**
     * Create a predicate for filtering by confidence level
     */
    public static Predicate<PricePrediction> createConfidenceFilter(double minConfidence) {
        return prediction -> prediction.confidence() >= minConfidence;
    }

    /**
     * Apply multiple filters to predictions using Stream API
     */
    public List<PricePrediction> filterPredictions(
            List<PricePrediction> predictions,
            String productFilter,
            LocalDate startDate,
            LocalDate endDate,
            double minConfidence) {

        return predictions.stream()
                .filter(createProductFilter(productFilter))
                .filter(createPredictionDateFilter(startDate, endDate))
                .filter(createConfidenceFilter(minConfidence))
                .collect(Collectors.toList());
    }

    /**
     * Apply multiple filters to export data using Stream API
     */
    public List<ExportData> filterExportData(
            List<ExportData> exportData,
            String productFilter,
            String countryFilter,
            LocalDate startDate,
            LocalDate endDate) {

        // Convert product name to ProductType if needed
        ProductType productType = null;
        if (!"All Products".equals(productFilter) && productFilter != null) {
            for (ProductType type : ProductType.values()) {
                if (type.getFrenchName().equals(productFilter)) {
                    productType = type;
                    break;
                }
            }
        }

        final ProductType finalProductType = productType;

        return exportData.stream()
                .filter(data -> finalProductType == null || data.productType() == finalProductType)
                .filter(createCountryFilter(countryFilter))
                .filter(createDateRangeFilter(startDate, endDate))
                .collect(Collectors.toList());
    }

    /**
     * Create a composite predicate that combines multiple filters
     * Demonstrates functional interface composition
     */
    @SafeVarargs
    public static <T> Predicate<T> combineFilters(Predicate<T>... filters) {
        Predicate<T> combined = filters[0];
        for (int i = 1; i < filters.length; i++) {
            combined = combined.and(filters[i]);
        }
        return combined;
    }

    /**
     * Count filtered results
     */
    public long countFilteredResults(List<PricePrediction> predictions, Predicate<PricePrediction> filter) {
        return predictions.stream()
                .filter(filter)
                .count();
    }

    /**
     * Get filtered statistics
     */
    public String getFilterSummary(
            List<PricePrediction> predictions,
            String productFilter,
            LocalDate startDate,
            LocalDate endDate) {

        long total = predictions.size();
        long filtered = filterPredictions(predictions, productFilter, startDate, endDate, 0.0).size();

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Total: %d | Filtered: %d", total, filtered));

        if (!"All Products".equals(productFilter)) {
            summary.append(" | Product: ").append(productFilter);
        }

        if (startDate != null && endDate != null) {
            summary.append(" | Date: ").append(startDate).append(" to ").append(endDate);
        }

        return summary.toString();
    }
}