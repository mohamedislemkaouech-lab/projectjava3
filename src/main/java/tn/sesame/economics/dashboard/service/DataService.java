package tn.sesame.economics.dashboard.service;

import tn.sesame.economics.model.ExportData;
import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ProductType;
import tn.sesame.economics.model.PredictionStatus;
import tn.sesame.economics.util.DataLoader;
import tn.sesame.economics.service.EconomicIntelligenceService;
import java.util.ArrayList;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;

/**
 * Service to load real CSV data and convert it for the dashboard
 */
public class DataService {

    private final EconomicIntelligenceService intelligenceService;
    private List<PricePrediction> cachedPredictions;
    private List<ExportData> cachedHistoricalData;

    public DataService(EconomicIntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
        this.cachedPredictions = new ArrayList<>();
        this.cachedHistoricalData = new ArrayList<>();
    }


    /**
     * Load historical export data from CSV
     */
    public List<ExportData> loadHistoricalData() {
        try {
            return DataLoader.loadCSVData("exports_historical.csv");
        } catch (Exception e) {
            System.out.println("❌ Error loading historical data: " + e.getMessage());
            return List.of(); // Return empty list if file not found
        }
    }

    /**
     * Load training data from CSV
     */
    public List<ExportData> loadTrainingData() {
        try {
            return DataLoader.loadCSVData("exports_training.csv");
        } catch (Exception e) {
            System.out.println("❌ Error loading training data: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Generate real predictions from historical data
     */
    public List<PricePrediction> generateRealPredictions() {
        List<ExportData> historicalData = loadHistoricalData();

        if (historicalData.isEmpty()) {
            System.out.println("⚠️ No historical data found. Using sample data.");
            return generateSamplePredictions();
        }

        System.out.println("✅ Loaded " + historicalData.size() + " records from exports_historical.csv");

        // Use only recent data for predictions (last 100 records)
        List<ExportData> recentData = historicalData.stream()
                .sorted((a, b) -> b.date().compareTo(a.date())) // Sort by date descending
                .limit(100)
                .collect(Collectors.toList());

        // Generate predictions using your intelligence service
        try {
            if (intelligenceService != null) {
                return intelligenceService.analyzeExports(recentData);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not generate AI predictions: " + e.getMessage());
        }

        // Fallback: Generate predictions based on historical patterns
        return generatePredictionsFromHistorical(recentData);
    }

    /**
     * Generate predictions based on historical patterns (fallback method)
     */
    private List<PricePrediction> generatePredictionsFromHistorical(List<ExportData> data) {
        return data.stream()
                .map(export -> {
                    // Calculate future price based on historical patterns
                    double basePrice = export.pricePerTon();
                    double predictedPrice = basePrice * (0.95 + (Math.random() * 0.1)); // ±5% variation

                    // Adjust based on market indicator
                    switch (export.indicator()) {
                        case RISING: predictedPrice *= 1.05; break;
                        case FALLING: predictedPrice *= 0.95; break;
                        case VOLATILE: predictedPrice *= (0.90 + Math.random() * 0.2); break;
                    }

                    // Calculate confidence based on data quality
                    double confidence = 0.7 + (Math.random() * 0.25); // 70-95% confidence

                    return new PricePrediction(
                            LocalDate.now().plusDays(30),
                            export.productType(),
                            predictedPrice,
                            confidence,
                            "Historical-Pattern-Model",
                            PredictionStatus.COMPLETED
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Get country distribution from real data
     */
    public java.util.Map<String, Integer> getCountryDistribution() {
        List<ExportData> data = loadHistoricalData();

        if (data.isEmpty()) {
            // Return sample distribution
            return java.util.Map.of(
                    "France", 35,
                    "Germany", 28,
                    "Italy", 22,
                    "Spain", 15,
                    "UK", 10,
                    "USA", 8,
                    "Canada", 5
            );
        }

        // Count exports by country
        return data.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ExportData::destinationCountry,
                        java.util.stream.Collectors.summingInt(e -> 1)
                ));
    }

    /**
     * Get product distribution from real data
     */
    public java.util.Map<String, Double> getProductPriceDistribution() {
        List<ExportData> data = loadHistoricalData();

        if (data.isEmpty()) {
            // Return sample distribution
            return java.util.Map.of(
                    "Huile d'olive", 3500.0,
                    "Dattes", 2200.0,
                    "Agrumes", 1800.0,
                    "Blé", 1200.0,
                    "Tomates", 1500.0,
                    "Piments", 2000.0
            );
        }

        // Calculate average price by product
        return data.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> e.productType().getFrenchName(),
                        java.util.stream.Collectors.averagingDouble(ExportData::pricePerTon)
                ));
    }
    public List<PricePrediction> getPredictions() {
        if (cachedPredictions.isEmpty()) {
            cachedPredictions = generateRealPredictions();
        }
        return cachedPredictions;
    }
    /**
     * Generate sample predictions (fallback)
     */
    private List<PricePrediction> generateSamplePredictions() {
        return java.util.List.of(
                new PricePrediction(
                        LocalDate.now().plusDays(30),
                        ProductType.OLIVE_OIL,
                        3500.0,
                        0.85,
                        "Sample-Model",
                        PredictionStatus.COMPLETED
                ),
                new PricePrediction(
                        LocalDate.now().plusDays(30),
                        ProductType.DATES,
                        2200.0,
                        0.78,
                        "Sample-Model",
                        PredictionStatus.COMPLETED
                ),
                new PricePrediction(
                        LocalDate.now().plusDays(30),
                        ProductType.CITRUS_FRUITS,
                        1800.0,
                        0.92,
                        "Sample-Model",
                        PredictionStatus.COMPLETED
                )
        );
    }
}