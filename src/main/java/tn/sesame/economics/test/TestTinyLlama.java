package tn.sesame.economics.test;

import tn.sesame.economics.integration.TinyLlamaService;
import tn.sesame.economics.model.*;
import tn.sesame.economics.service.ReportGenerator;

import java.time.LocalDate;
import java.util.List;

public class TestTinyLlama {
    public static void main(String[] args) {
        System.out.println("🧪 Test d'intégration TinyLlama\n");

        TinyLlamaService service = new TinyLlamaService();

        // Test 1: Vérifier la connexion
        System.out.println("1. Test de connexion...");
        String connectionTest = service.testConnection();
        System.out.println(connectionTest);

        // Test 2: Vérifier si disponible
        System.out.println("\n2. Vérification disponibilité...");
        boolean available = service.isOllamaAvailable();
        System.out.println("Disponible: " + (available ? "✅ OUI" : "❌ NON"));

        if (available) {
            System.out.println("\n3. Test d'analyse rapide...");
            // Créer des données de test simples
            List<PricePrediction> testData = List.of(
                    new PricePrediction(
                            LocalDate.now().plusDays(30),
                            ProductType.OLIVE_OIL,
                            2500.50,
                            0.85,
                            "Test-Model",
                            PredictionStatus.COMPLETED
                    ),
                    new PricePrediction(
                            LocalDate.now().plusDays(30),
                            ProductType.DATES,
                            1800.75,
                            0.78,
                            "Test-Model",
                            PredictionStatus.COMPLETED
                    )
            );

            System.out.println("Données de test créées (2 prédictions)");

            // Test avec la méthode correcte (generateReport avec format enum)
            System.out.println("\n4. Test generateReport avec format MARKDOWN:");
            String report = service.generateReport(testData, ReportGenerator.ReportFormat.MARKDOWN);
            System.out.println("\n" + report);

            System.out.println("\n5. Test generateMarketReport:");
            String marketReport = service.generateMarketReport(testData);
            System.out.println("\n" + marketReport);

            System.out.println("\n6. Test generateSummaryReport:");
            String summary = service.generateSummaryReport(testData);
            System.out.println("\n" + summary);
        }

        System.out.println("\n✅ Test terminé!");
    }
}