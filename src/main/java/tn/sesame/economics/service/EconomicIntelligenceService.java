package tn.sesame.economics.service;

import tn.sesame.economics.ai.BaseAIModel;
import tn.sesame.economics.model.*;
import tn.sesame.economics.exception.PredictionException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

/**
 * Service d'intelligence économique qui orchestre les modèles d'IA
 * et la génération de rapports.
 */
public class EconomicIntelligenceService {

    private static final Logger LOGGER = Logger.getLogger(EconomicIntelligenceService.class.getName());
    private final BaseAIModel predictionModel;
    private final ReportGenerator reportGenerator;

    public EconomicIntelligenceService(BaseAIModel predictionModel, ReportGenerator reportGenerator) {
        this.predictionModel = predictionModel;
        this.reportGenerator = reportGenerator;
    }

    // Add this method:
    public ReportGenerator getReportGenerator() {
        return this.reportGenerator;
    }

    public BaseAIModel getPredictionModel() {
        return this.predictionModel;
    }

    public List<PricePrediction> analyzeExports(List<ExportData> exportData) throws PredictionException {
        LOGGER.info("Analyse de " + exportData.size() + " enregistrements...");

        return exportData.stream()
                .map(data -> {
                    try {
                        return predictionModel.predictPrice(data);
                    } catch (Exception e) {
                        LOGGER.warning("Erreur de prédiction pour " + data.productType() + ": " + e.getMessage());
                        // Retourner une prédiction par défaut en cas d'erreur
                        return new PricePrediction(
                                data.date().plusMonths(1),
                                data.productType(),
                                data.pricePerTon() * 1.05, // +5% par défaut
                                0.5, // Confiance faible
                                predictionModel.getModelName(),
                                PredictionStatus.FAILED  // Changed from ERROR to FAILED
                        );
                    }
                })
                .collect(Collectors.toList());
    }

    public String generateIntelligenceReport(List<PricePrediction> predictions) {
        LOGGER.info("Génération du rapport d'intelligence...");

        // Si le reportGenerator est TinyLlamaService, on peut utiliser la méthode spécifique
        if (reportGenerator instanceof tn.sesame.economics.integration.TinyLlamaService) {
            return reportGenerator.generateMarketReport(predictions);
        } else {
            // Sinon, générer un rapport simple
            return generateSimpleReport(predictions);
        }
    }

    private String generateSimpleReport(List<PricePrediction> predictions) {
        StringBuilder report = new StringBuilder();
        report.append("📊 RAPPORT D'INTELLIGENCE ÉCONOMIQUE\n");
        report.append("=".repeat(40)).append("\n\n");

        report.append("Analyse basée sur ").append(predictions.size()).append(" prédictions\n\n");

        // Group by product type
        predictions.stream()
                .collect(Collectors.groupingBy(PricePrediction::productType, Collectors.toList()))
                .forEach((product, productPredictions) -> {
                    double avgPrice = productPredictions.stream()
                            .mapToDouble(PricePrediction::predictedPrice)
                            .average()
                            .orElse(0.0);

                    report.append(String.format("• %s: Prix moyen prédit: %.2f TND/tonne (%d prédictions)%n",
                            product.getFrenchName(), avgPrice, productPredictions.size()));
                });

        return report.toString();
    }
}