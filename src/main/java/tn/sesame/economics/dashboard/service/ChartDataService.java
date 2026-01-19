package tn.sesame.economics.dashboard.service;

import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ProductType;
import tn.sesame.economics.model.ExportData;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to prepare data for chart visualization
 */
public class ChartDataService {

    /**
     * Prepare product price distribution for bar/pie charts
     */
    public Map<String, Double> prepareProductPriceChart(List<PricePrediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return Collections.emptyMap();
        }

        return predictions.stream()
                .collect(Collectors.groupingBy(
                        prediction -> prediction.productType().getFrenchName(),
                        Collectors.averagingDouble(PricePrediction::predictedPrice)
                ))
                .entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // Sort by price descending
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new // Maintain order
                ));
    }

    /**
     * Prepare confidence distribution chart
     */
    public Map<String, Double> prepareConfidenceChart(List<PricePrediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Long> confidenceCategories = new LinkedHashMap<>();

        // Categorize by confidence level
        long high = predictions.stream()
                .filter(p -> p.confidence() >= 0.8)
                .count();
        long medium = predictions.stream()
                .filter(p -> p.confidence() >= 0.6 && p.confidence() < 0.8)
                .count();
        long low = predictions.stream()
                .filter(p -> p.confidence() < 0.6)
                .count();

        confidenceCategories.put("High (≥80%)", high);
        confidenceCategories.put("Medium (60-80%)", medium);
        confidenceCategories.put("Low (<60%)", low);

        // Convert counts to percentages
        double total = predictions.size();
        Map<String, Double> result = new LinkedHashMap<>();
        confidenceCategories.forEach((category, count) -> {
            if (count > 0) {
                result.put(category, (count / total) * 100);
            }
        });

        return result;
    }

    /**
     * Prepare time series data for line chart (monthly averages)
     */
    public Map<String, Double> prepareTimeSeriesChart(List<ExportData> historicalData) {
        if (historicalData == null || historicalData.isEmpty()) {
            return createSampleTimeSeries();
        }

        // Group by month and calculate average price
        Map<String, Double> timeSeries = historicalData.stream()
                .collect(Collectors.groupingBy(
                        data -> {
                            Month month = data.date().getMonth();
                            return month.toString().substring(0, 3); // "JAN", "FEB", etc.
                        },
                        Collectors.averagingDouble(ExportData::pricePerTon)
                ));

        // Order by month
        List<String> monthOrder = Arrays.asList(
                "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
        );

        return timeSeries.entrySet().stream()
                .filter(entry -> monthOrder.contains(entry.getKey()))
                .sorted(Comparator.comparingInt(e -> monthOrder.indexOf(e.getKey())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Prepare country distribution chart
     */
    public Map<String, Double> prepareCountryDistributionChart(List<ExportData> historicalData) {
        if (historicalData == null || historicalData.isEmpty()) {
            return createSampleCountryDistribution();
        }

        // Count exports by country
        Map<String, Long> countryCounts = historicalData.stream()
                .collect(Collectors.groupingBy(
                        ExportData::destinationCountry,
                        Collectors.counting()
                ));

        // Convert to percentages
        double total = historicalData.size();
        return countryCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue())) // Sort by count descending
                .limit(8) // Top 8 countries
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (entry.getValue() / total) * 100,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Prepare comparative analysis: Product price comparison
     */
    public Map<String, Double> prepareProductComparisonChart(List<PricePrediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return Collections.emptyMap();
        }

        // Calculate average price, min, and max for each product
        Map<String, Double> result = new LinkedHashMap<>();

        for (ProductType productType : ProductType.values()) {
            List<PricePrediction> productPredictions = predictions.stream()
                    .filter(p -> p.productType() == productType)
                    .collect(Collectors.toList());

            if (!productPredictions.isEmpty()) {
                double avgPrice = productPredictions.stream()
                        .mapToDouble(PricePrediction::predictedPrice)
                        .average()
                        .orElse(0);

                result.put(productType.getFrenchName(), avgPrice);
            }
        }

        return result.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Prepare seasonal pattern chart
     */
    public Map<String, Double> prepareSeasonalPatternChart(List<ExportData> historicalData) {
        Map<String, Double> seasonalPattern = new LinkedHashMap<>();

        // Tunisian agricultural seasons
        seasonalPattern.put("Winter (Dec-Feb)", 0.0);
        seasonalPattern.put("Spring (Mar-May)", 0.0);
        seasonalPattern.put("Summer (Jun-Aug)", 0.0);
        seasonalPattern.put("Autumn (Sep-Nov)", 0.0);

        if (historicalData == null || historicalData.isEmpty()) {
            // Sample seasonal pattern for Tunisian exports
            seasonalPattern.put("Winter (Dec-Feb)", 85.0); // Olive harvest season
            seasonalPattern.put("Spring (Mar-May)", 95.0); // Citrus season
            seasonalPattern.put("Summer (Jun-Aug)", 75.0); // Lower exports
            seasonalPattern.put("Autumn (Sep-Nov)", 105.0); // Date harvest season
            return seasonalPattern;
        }

        // Calculate actual seasonal averages
        Map<String, List<Double>> seasonPrices = new HashMap<>();

        for (ExportData data : historicalData) {
            int month = data.date().getMonthValue();
            String season = getSeason(month);
            seasonPrices.computeIfAbsent(season, k -> new ArrayList<>())
                    .add(data.pricePerTon());
        }

        // Calculate averages
        seasonPrices.forEach((season, prices) -> {
            double average = prices.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);
            seasonalPattern.put(season, average);
        });

        return seasonalPattern;
    }

    private String getSeason(int month) {
        if (month >= 12 || month <= 2) return "Winter (Dec-Feb)";
        if (month >= 3 && month <= 5) return "Spring (Mar-May)";
        if (month >= 6 && month <= 8) return "Summer (Jun-Aug)";
        return "Autumn (Sep-Nov)";
    }

    private Map<String, Double> createSampleTimeSeries() {
        Map<String, Double> sample = new LinkedHashMap<>();
        String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        Random random = new Random();

        double base = 2000;
        for (String month : months) {
            sample.put(month, base + random.nextDouble() * 1000);
            base += 50; // Slight upward trend
        }

        return sample;
    }

    private Map<String, Double> createSampleCountryDistribution() {
        Map<String, Double> sample = new LinkedHashMap<>();
        sample.put("France", 35.0);
        sample.put("Germany", 25.0);
        sample.put("Italy", 18.0);
        sample.put("Spain", 12.0);
        sample.put("UK", 6.0);
        sample.put("USA", 4.0);
        return sample;
    }

    /**
     * Get available chart types with descriptions
     */
    public Map<String, String> getAvailableCharts() {
        Map<String, String> charts = new LinkedHashMap<>();
        charts.put("PRODUCT_PRICES", "Product Price Distribution");
        charts.put("CONFIDENCE_LEVELS", "Prediction Confidence Levels");
        charts.put("TIME_SERIES", "Monthly Price Trends");
        charts.put("COUNTRY_DIST", "Export Country Distribution");
        charts.put("PRODUCT_COMPARE", "Product Price Comparison");
        charts.put("SEASONAL", "Seasonal Export Patterns");
        return charts;
    }
}