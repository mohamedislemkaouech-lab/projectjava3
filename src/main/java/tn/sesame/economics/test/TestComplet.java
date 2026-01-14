// Créer TestComplet.java
public class TestComplet {
    public static void main(String[] args) {
        System.out.println("🧪 TESTS COMPLETS DU SYSTÈME");
        System.out.println("=".repeat(50));

        // Test 1: Flux complet avec DJL
        testFluxComplet("DJL", new DJLPredictionService());

        // Test 2: Flux complet avec ONNX
        testFluxComplet("ONNX", new ONNXRuntimeService());

        // Test 3: Flux complet avec Simple
        testFluxComplet("Simple", new SimpleLinearPredictionService());

        // Test 4: Intégration TinyLlama
        testTinyLlamaIntegration();
    }
}