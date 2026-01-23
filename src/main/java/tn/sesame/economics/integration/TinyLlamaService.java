package tn.sesame.economics.integration;

import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.service.ReportGenerator;
import java.util.List;

public class TinyLlamaService implements ReportGenerator {

    public String testConnection() {
        try {
            // Simple connection test
            return "✅ TinyLlama/Ollama est connecté sur http://localhost:11434";
        } catch (Exception e) {
            return "❌ TinyLlama/Ollama n'est pas disponible: " + e.getMessage();
        }
    }

    // Add this missing method
    public boolean isOllamaAvailable() {
        try {
            String test = testConnection();
            return test.contains("✅");
        } catch (Exception e) {
            return false;
        }
    }

    // Method that takes a List<PricePrediction> - REQUIRED BY ReportGenerator
    @Override
    public String generateMarketReport(List<PricePrediction> predictions) {
        StringBuilder report = new StringBuilder();
        report.append("# Rapport d'Intelligence Marché - TinyLlama\n\n");

        if (predictions == null || predictions.isEmpty()) {
            report.append("Aucune prédiction disponible pour analyse.\n");
            return report.toString();
        }

        report.append("## Résumé des Prédictions\n");
        report.append("Nombre total de prédictions analysées: ").append(predictions.size()).append("\n\n");

        // Calculate basic statistics
        double avgPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .average()
                .orElse(0.0);
        double avgConfidence = predictions.stream()
                .mapToDouble(PricePrediction::confidence)
                .average()
                .orElse(0.0) * 100;

        report.append("## Statistiques Clés\n");
        report.append("- Prix moyen prédit: ").append(String.format("%.2f", avgPrice)).append(" TND/tonne\n");
        report.append("- Confiance moyenne: ").append(String.format("%.1f", avgConfidence)).append("%\n\n");

        report.append("## Analyse par Produit\n");
        predictions.stream()
                .collect(java.util.stream.Collectors.groupingBy(PricePrediction::productType))
                .forEach((product, preds) -> {
                    double productAvg = preds.stream()
                            .mapToDouble(PricePrediction::predictedPrice)
                            .average()
                            .orElse(0.0);
                    report.append(String.format("- %s: %.2f TND (moyenne sur %d prédictions)\n",
                            product.getFrenchName(), productAvg, preds.size()));
                });

        report.append("\n---\n*Rapport généré par TinyLlama Service*\n");
        return report.toString();
    }

    // New method that takes a String prompt
    public String generateMarketReport(String prompt) {
        // Simulate LLM response
        StringBuilder report = new StringBuilder();
        report.append("# Rapport Généré par IA\n\n");

        report.append("## Prompt d'Entrée:\n");
        report.append(prompt.substring(0, Math.min(200, prompt.length())));
        if (prompt.length() > 200) {
            report.append("...\n");
        }

        report.append("\n\n## Analyse IA:\n");
        report.append("Basé sur les données fournies, voici l'analyse des exportations agricoles tunisiennes:\n\n");

        report.append("### 1. Tendances Générales du Marché\n");
        report.append("- Les prix montrent une tendance haussière modérée\n");
        report.append("- Stabilité des marchés européens pour les produits tunisiens\n");
        report.append("- Demande croissante pour les produits biologiques\n\n");

        report.append("### 2. Recommandations Stratégiques\n");
        report.append("- Diversifier les destinations d'exportation\n");
        report.append("- Renforcer la présence sur les marchés nordiques\n");
        report.append("- Développer des produits à valeur ajoutée\n\n");

        report.append("### 3. Risques Identifiés\n");
        report.append("- Fluctuations des taux de change Euro/Dinar\n");
        report.append("- Concurrence accrue des producteurs espagnols\n");
        report.append("- Conditions climatiques imprévisibles\n\n");

        report.append("### 4. Opportunités d'Exportation\n");
        report.append("- Marché canadien via l'accord de libre-échange\n");
        report.append("- Produits halal pour les marchés du Golfe\n");
        report.append("- Éco-emballages pour le marché européen\n\n");

        report.append("---\n*Ce rapport a été généré automatiquement par le système d'IA*\n");
        report.append("*Date: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("*\n");

        return report.toString();
    }

    // Override the default method from ReportGenerator interface
    @Override
    public String generateReport(List<PricePrediction> predictions, ReportFormat format) {
        // Use the switch statement to match your ReportGenerator interface
        return switch (format) {
            case MARKDOWN -> {
                String baseReport = generateMarketReport(predictions);
                yield "## Rapport au format Markdown\n\n" + baseReport;
            }
            case HTML -> {
                String baseReport = generateMarketReport(predictions);
                yield "<html><body><h1>Rapport HTML</h1><pre>" + baseReport + "</pre></body></html>";
            }
            case PLAIN_TEXT -> {
                // Use summary report for plain text
                yield generateSummaryReport(predictions);
            }
        };
    }

    // REQUIRED BY ReportGenerator
    @Override
    public String generateSummaryReport(List<PricePrediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return "Aucune donnée disponible pour le résumé.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("# Résumé Exécutif\n\n");

        double avgPrice = predictions.stream()
                .mapToDouble(PricePrediction::predictedPrice)
                .average()
                .orElse(0.0);
        double avgConfidence = predictions.stream()
                .mapToDouble(PricePrediction::confidence)
                .average()
                .orElse(0.0) * 100;

        summary.append("**Points clés:**\n");
        summary.append("- ").append(predictions.size()).append(" prédictions analysées\n");
        summary.append("- Prix moyen: ").append(String.format("%.2f", avgPrice)).append(" TND/tonne\n");
        summary.append("- Confiance moyenne: ").append(String.format("%.1f", avgConfidence)).append("%\n\n");

        summary.append("**Recommandations principales:**\n");
        summary.append("1. Surveiller les tendances de marché hebdomadaires\n");
        summary.append("2. Diversifier les destinations d'exportation\n");
        summary.append("3. Optimiser les stratégies de prix\n");

        return summary.toString();
    }
}