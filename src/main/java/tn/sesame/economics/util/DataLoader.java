package tn.sesame.economics.util;

import tn.sesame.economics.model.*;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DataLoader {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Charge les données CSV et les convertit en objets ExportData
     */
    public static List<ExportData> loadCSVData(String fileName) {
        List<ExportData> data = new ArrayList<>();

        // Try multiple possible locations
        String[] possiblePaths = {
                fileName,                                      // Current directory
                "data/" + fileName,                            // data folder
                "src/main/resources/data/" + fileName,         // Maven resources
                "src/main/resources/" + fileName,              // Resources root
                "resources/data/" + fileName,                  // Compiled resources
                System.getProperty("user.dir") + "/" + fileName,
                System.getProperty("user.dir") + "/data/" + fileName
        };

        Path foundPath = null;

        for (String pathStr : possiblePaths) {
            try {
                Path path = Paths.get(pathStr);
                if (Files.exists(path)) {
                    foundPath = path;
                    log.info("✅ Found CSV file at: {}", path.toAbsolutePath());
                    break;
                }
            } catch (Exception e) {
                // Try next path
            }
        }

        if (foundPath == null) {
            log.error("❌ File '{}' not found in any location!", fileName);
            log.error("💡 Searched in:");
            for (String path : possiblePaths) {
                log.error("   - {}", path);
            }
            return data;
        }

        try {
            List<String> lines = Files.readAllLines(foundPath);
            log.info("📄 File has {} lines", lines.size());

            boolean isFirstLine = true;
            int successCount = 0;
            int errorCount = 0;

            for (String line : lines) {
                if (isFirstLine) {
                    log.info("📋 Header: {}", line);
                    isFirstLine = false;
                    continue;
                }

                if (!line.trim().isEmpty()) {
                    ExportData exportData = parseCSVLine(line);
                    if (exportData != null) {
                        data.add(exportData);
                        successCount++;
                    } else {
                        errorCount++;
                    }
                }
            }

            log.info("✅ Successfully loaded {} records from {}", successCount, fileName);
            if (errorCount > 0) {
                log.warn("⚠️  Failed to parse {} lines", errorCount);
            }

        } catch (IOException e) {
            log.error("❌ Error reading file: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ Error parsing CSV: {}", e.getMessage());
        }

        return data;
    }

    /**
     * Parse une ligne CSV en objet ExportData
     * Format: date,product_type,price_per_ton,volume,destination_country,market_indicator,price_volatility,exchange_rate
     */
    private static ExportData parseCSVLine(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length != 8) {
                log.warn("Ligne invalide ({} champs): {}", parts.length, line);
                return null;
            }

            return new ExportData(
                    LocalDate.parse(parts[0].trim(), DATE_FORMATTER),
                    ProductType.valueOf(parts[1].trim()),
                    Double.parseDouble(parts[2].trim()),
                    Double.parseDouble(parts[3].trim()),
                    parts[4].trim(),
                    MarketIndicator.valueOf(parts[5].trim()),
                    Double.parseDouble(parts[6].trim()),  // price_volatility
                    Double.parseDouble(parts[7].trim())   // exchange_rate_TND_USD
            );
        } catch (Exception e) {
            log.error("Erreur de parsing de la ligne: {} - {}", line, e.getMessage());
            return null;
        }
    }

    /**
     * Encode les caractéristiques d'une exportation en vecteur numérique.
     * Mise à jour pour inclure les nouveaux champs.
     */
    public static double[] encodeFeatures(ExportData data) {
        // Augmenter la taille du vecteur pour inclure les nouvelles features
        double[] features = new double[12];

        // 1. Encodage du mois (0-11)
        features[0] = data.date().getMonthValue() / 12.0;

        // 2. Encodage du produit (one-hot simplifié)
        int productIndex = data.productType().ordinal();
        features[1 + productIndex % 5] = 1.0;

        // 3. Encodage du volume (normalisé)
        features[6] = data.volume() / 1000.0;

        // 4. Encodage du pays (simplifié)
        int countryHash = Math.abs(data.destinationCountry().hashCode() % 10);
        features[7] = countryHash / 10.0;

        // 5. Encodage de l'indicateur de marché
        features[8] = data.indicator().ordinal() / 4.0;

        // 6. Année normalisée (0-1 sur 20 ans)
        int year = data.date().getYear();
        features[9] = (year - 2005) / 20.0;

        // 7. Volatilité des prix
        features[10] = data.priceVolatility();

        // 8. Taux de change
        features[11] = data.exchangeRateTNDUSD() / 4.0;

        return features;
    }

    /**
     * Affiche les statistiques du dataset avec les nouveaux champs.
     */
    public static void displayDatasetStatistics(List<ExportData> dataset) {
        if (dataset.isEmpty()) {
            System.out.println("⚠️  Dataset vide");
            return;
        }

        System.out.println("\n📊 STATISTIQUES DU DATASET (8 champs)");
        System.out.println("=".repeat(60));

        // Nombre total d'enregistrements
        System.out.printf("Nombre total d'exportations: %,d%n", dataset.size());

        // Période couverte
        LocalDate minDate = dataset.stream()
                .map(ExportData::date)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        LocalDate maxDate = dataset.stream()
                .map(ExportData::date)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
        System.out.printf("Période: %s à %s%n", minDate, maxDate);

        // Prix moyen
        double avgPrice = dataset.stream()
                .mapToDouble(ExportData::pricePerTon)
                .average()
                .orElse(0.0);
        System.out.printf("Prix moyen par tonne: %.2f TND%n", avgPrice);

        // Volume total
        double totalVolume = dataset.stream()
                .mapToDouble(ExportData::volume)
                .sum();
        System.out.printf("Volume total exporté: %.2f tonnes%n", totalVolume);

        // Volatilité moyenne
        double avgVolatility = dataset.stream()
                .mapToDouble(ExportData::priceVolatility)
                .average()
                .orElse(0.0);
        System.out.printf("Volatilité moyenne des prix: %.4f%n", avgVolatility);

        // Taux de change moyen
        double avgExchangeRate = dataset.stream()
                .mapToDouble(ExportData::exchangeRateTNDUSD)
                .average()
                .orElse(0.0);
        System.out.printf("Taux de change moyen TND/USD: %.4f%n", avgExchangeRate);

        // Distribution par produit
        System.out.println("\n📦 DISTRIBUTION PAR PRODUIT:");
        Map<ProductType, Long> productCount = dataset.stream()
                .collect(Collectors.groupingBy(ExportData::productType, Collectors.counting()));

        productCount.forEach((product, count) -> {
            double percentage = (count * 100.0) / dataset.size();
            System.out.printf("  • %-15s: %3d (%.1f%%)%n",
                    product.getFrenchName(), count, percentage);
        });

        // Distribution par pays
        System.out.println("\n🌍 TOP 5 PAYS DESTINATION:");
        Map<String, Long> countryCount = dataset.stream()
                .collect(Collectors.groupingBy(ExportData::destinationCountry, Collectors.counting()));

        countryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry ->
                        System.out.printf("  • %-12s: %3d exportations%n", entry.getKey(), entry.getValue()));
    }

    /**
     * NOUVELLE MÉTHODE: Export predictions to CSV file
     */
    public static void exportPredictionsToCSV(List<PricePrediction> predictions, String fileName) throws IOException {
        if (predictions.isEmpty()) {
            throw new IOException("No predictions to export");
        }

        StringBuilder csv = new StringBuilder();
        // Header
        csv.append("prediction_date,product_type,predicted_price,confidence,model_name,status\n");

        // Data rows
        for (PricePrediction pred : predictions) {
            csv.append(String.format("%s,%s,%.2f,%.4f,%s,%s%n",
                    pred.predictionDate(),
                    pred.productType().name(),
                    pred.predictedPrice(),
                    pred.confidence(),
                    pred.modelName(),
                    pred.status().name()));
        }

        Files.writeString(Paths.get(fileName), csv.toString());
        log.info("✅ Exported {} predictions to {}", predictions.size(), fileName);
    }

    /**
     * NOUVELLE MÉTHODE: Prepare data for training - converts ExportData to feature vectors and targets.
     */
    public static Map<String, Object> prepareDataForTraining(List<ExportData> data) {
        List<double[]> features = new ArrayList<>();
        double[] targets = new double[data.size()];

        for (int i = 0; i < data.size(); i++) {
            ExportData export = data.get(i);
            features.add(encodeFeatures(export));
            targets[i] = export.pricePerTon();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("features", features);
        result.put("targets", targets);
        return result;
    }

    /**
     * Normalize features to [0, 1] range
     */
    public static List<double[]> normalizeFeatures(List<double[]> features) {
        if (features.isEmpty()) return features;

        int numFeatures = features.get(0).length;
        double[] mins = new double[numFeatures];
        double[] maxs = new double[numFeatures];

        Arrays.fill(mins, Double.MAX_VALUE);
        Arrays.fill(maxs, Double.MIN_VALUE);

        // Find min and max for each feature
        for (double[] feature : features) {
            for (int i = 0; i < numFeatures; i++) {
                mins[i] = Math.min(mins[i], feature[i]);
                maxs[i] = Math.max(maxs[i], feature[i]);
            }
        }

        // Normalize
        List<double[]> normalized = new ArrayList<>();
        for (double[] feature : features) {
            double[] norm = new double[numFeatures];
            for (int i = 0; i < numFeatures; i++) {
                double range = maxs[i] - mins[i];
                norm[i] = range > 0 ? (feature[i] - mins[i]) / range : 0.0;
            }
            normalized.add(norm);
        }

        return normalized;
    }

    /**
     * Check file locations - utility method
     */
    public static void checkFileLocations() {
        System.out.println("\n=== VÉRIFICATION DES FICHIERS ===");

        String[] files = {
                "exports_historical.csv",
                "exports_training.csv",
                "exports_test.csv"
        };

        for (String file : files) {
            String[] paths = {
                    file,
                    "data/" + file,
                    "src/main/resources/data/" + file,
                    "resources/data/" + file,
                    "src/main/resources/" + file,
                    "resources/" + file
            };

            System.out.println("\nRecherche de: " + file);
            boolean found = false;

            for (String path : paths) {
                Path filePath = Paths.get(path);
                if (Files.exists(filePath)) {
                    System.out.println("✓ Trouvé à: " + filePath.toAbsolutePath());
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println("❌ Fichier non trouvé");
            }
        }
    }
}