package tn.sesame.economics.dashboard.service;

import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.ExportData;
import tn.sesame.economics.model.ProductType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Service for exporting data in various formats (CSV, JSON)
 * Demonstrates file I/O and data serialization
 */
public class ExportService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Export predictions to CSV file
     */
    public String exportPredictionsToCSV(List<PricePrediction> predictions, String fileName) {
        if (predictions == null || predictions.isEmpty()) {
            return "No data to export";
        }

        try {
            // Generate filename with timestamp if not provided
            String finalFileName = fileName;
            if (fileName == null || fileName.isEmpty()) {
                finalFileName = String.format("predictions_export_%s.csv",
                        LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            }

            // Ensure .csv extension
            if (!finalFileName.endsWith(".csv")) {
                finalFileName += ".csv";
            }

            // Create export directory if it doesn't exist
            Path exportDir = Paths.get("exports");
            if (!Files.exists(exportDir)) {
                Files.createDirectories(exportDir);
            }

            Path filePath = exportDir.resolve(finalFileName);

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                // Write CSV header
                writer.write("prediction_date,product_type,product_french_name,predicted_price," +
                        "confidence,model_name,status\n");

                // Write data rows
                for (PricePrediction prediction : predictions) {
                    writer.write(String.format("%s,%s,%s,%.2f,%.3f,%s,%s\n",
                            prediction.predictionDate(),
                            prediction.productType().name(),
                            prediction.productType().getFrenchName(),
                            prediction.predictedPrice(),
                            prediction.confidence(),
                            prediction.modelName(),
                            prediction.status().name()
                    ));
                }

                writer.flush();

                String message = String.format("✅ Successfully exported %d predictions to: %s",
                        predictions.size(), filePath.toAbsolutePath());
                System.out.println(message);
                return message;

            } catch (IOException e) {
                String error = "❌ Error writing CSV file: " + e.getMessage();
                System.err.println(error);
                return error;
            }

        } catch (Exception e) {
            String error = "❌ Export failed: " + e.getMessage();
            System.err.println(error);
            e.printStackTrace();
            return error;
        }
    }

    /**
     * Export export data to CSV (historical data)
     */
    public String exportHistoricalDataToCSV(List<ExportData> exportData, String fileName) {
        if (exportData == null || exportData.isEmpty()) {
            return "No historical data to export";
        }

        try {
            String finalFileName = fileName;
            if (fileName == null || fileName.isEmpty()) {
                finalFileName = String.format("historical_export_%s.csv",
                        LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            }

            if (!finalFileName.endsWith(".csv")) {
                finalFileName += ".csv";
            }

            Path exportDir = Paths.get("exports");
            if (!Files.exists(exportDir)) {
                Files.createDirectories(exportDir);
            }

            Path filePath = exportDir.resolve(finalFileName);

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                // Write CSV header
                writer.write("date,product_type,product_french_name,price_per_ton," +
                        "volume,destination_country,market_indicator\n");

                // Write data rows
                for (ExportData data : exportData) {
                    writer.write(String.format("%s,%s,%s,%.2f,%.2f,%s,%s\n",
                            data.date(),
                            data.productType().name(),
                            data.productType().getFrenchName(),
                            data.pricePerTon(),
                            data.volume(),
                            data.destinationCountry(),
                            data.indicator().name()
                    ));
                }

                writer.flush();

                String message = String.format("✅ Successfully exported %d historical records to: %s",
                        exportData.size(), filePath.toAbsolutePath());
                System.out.println(message);
                return message;

            } catch (IOException e) {
                String error = "❌ Error writing historical CSV: " + e.getMessage();
                System.err.println(error);
                return error;
            }

        } catch (Exception e) {
            String error = "❌ Historical export failed: " + e.getMessage();
            System.err.println(error);
            return error;
        }
    }

    /**
     * Export predictions to JSON file
     */
    public String exportPredictionsToJSON(List<PricePrediction> predictions, String fileName) {
        if (predictions == null || predictions.isEmpty()) {
            return "No data to export";
        }

        try {
            String finalFileName = fileName;
            if (fileName == null || fileName.isEmpty()) {
                finalFileName = String.format("predictions_export_%s.json",
                        LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            }

            if (!finalFileName.endsWith(".json")) {
                finalFileName += ".json";
            }

            Path exportDir = Paths.get("exports");
            if (!Files.exists(exportDir)) {
                Files.createDirectories(exportDir);
            }

            Path filePath = exportDir.resolve(finalFileName);

            // Prepare data for JSON serialization
            List<Map<String, Object>> jsonData = predictions.stream()
                    .map(prediction -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("prediction_date", prediction.predictionDate().toString());
                        map.put("product_type", prediction.productType().name());
                        map.put("product_french_name", prediction.productType().getFrenchName());
                        map.put("predicted_price", prediction.predictedPrice());
                        map.put("confidence", prediction.confidence());
                        map.put("model_name", prediction.modelName());
                        map.put("status", prediction.status().name());
                        map.put("confidence_percentage", String.format("%.1f%%", prediction.confidence() * 100));
                        return map;
                    })
                    .collect(Collectors.toList());

            // Add metadata
            Map<String, Object> fullJson = new HashMap<>();
            fullJson.put("export_timestamp", LocalDateTime.now().toString());
            fullJson.put("total_records", predictions.size());
            fullJson.put("data", jsonData);

            // Write JSON
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            objectMapper.writeValue(filePath.toFile(), fullJson);

            String message = String.format("✅ Successfully exported %d predictions to JSON: %s",
                    predictions.size(), filePath.toAbsolutePath());
            System.out.println(message);
            return message;

        } catch (Exception e) {
            String error = "❌ JSON export failed: " + e.getMessage();
            System.err.println(error);
            e.printStackTrace();
            return error;
        }
    }

    /**
     * Export dashboard statistics to JSON
     */
    public String exportStatisticsToJSON(Map<String, Object> statistics, String fileName) {
        try {
            String finalFileName = fileName;
            if (fileName == null || fileName.isEmpty()) {
                finalFileName = String.format("dashboard_statistics_%s.json",
                        LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            }

            if (!finalFileName.endsWith(".json")) {
                finalFileName += ".json";
            }

            Path exportDir = Paths.get("exports");
            if (!Files.exists(exportDir)) {
                Files.createDirectories(exportDir);
            }

            Path filePath = exportDir.resolve(finalFileName);

            // Add metadata
            statistics.put("export_timestamp", LocalDateTime.now().toString());
            statistics.put("export_format", "JSON");

            // Write JSON
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            objectMapper.writeValue(filePath.toFile(), statistics);

            return String.format("✅ Statistics exported to: %s", filePath.toAbsolutePath());

        } catch (Exception e) {
            return "❌ Statistics export failed: " + e.getMessage();
        }
    }

    /**
     * Export filtered data with user-selected format
     */
    public String exportFilteredData(List<PricePrediction> predictions, String format,
                                     String customFileName) {
        if (predictions == null || predictions.isEmpty()) {
            return "⚠️ No data to export";
        }

        System.out.println(String.format("📤 Exporting %d predictions as %s format",
                predictions.size(), format));

        switch (format.toUpperCase()) {
            case "CSV":
                return exportPredictionsToCSV(predictions, customFileName);

            case "JSON":
                return exportPredictionsToJSON(predictions, customFileName);

            default:
                return "❌ Unsupported export format: " + format + ". Use CSV or JSON.";
        }
    }

    /**
     * Get list of available export files
     */
    public List<String> getExportFiles() {
        try {
            Path exportDir = Paths.get("exports");
            if (!Files.exists(exportDir)) {
                Files.createDirectories(exportDir);
                return List.of("No export files yet");
            }

            return Files.list(exportDir)
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error listing export files: " + e.getMessage());
            return List.of("Error accessing export directory");
        }
    }

    /**
     * Get export directory path
     */
    public String getExportDirectory() {
        Path exportDir = Paths.get("exports");
        return exportDir.toAbsolutePath().toString();
    }
}