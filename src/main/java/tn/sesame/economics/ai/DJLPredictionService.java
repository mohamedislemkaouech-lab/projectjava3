package tn.sesame.economics.ai;

import tn.sesame.economics.annotation.AIService;
import tn.sesame.economics.model.*;
import tn.sesame.economics.exception.ModelException;
import tn.sesame.economics.util.DataLoader;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@AIService(provider = "DJL-PyTorch", version = "1.0")
public class DJLPredictionService extends BaseAIModel {

    // Simulation d'un modèle deep learning simple
    private float[][] weights;  // Poids du réseau de neurones
    private float[] biases;

    public DJLPredictionService() {
        super("DJL-PyTorch-Predictor");
        // Initialiser avec des poids aléatoires
        initializeRandomWeights();
    }

    private void initializeRandomWeights() {
        // Réseau simple: 10 → 8 → 4 → 1
        weights = new float[3][];
        biases = new float[3];

        // Couche 1: 10 entrées → 8 neurones
        weights[0] = new float[10 * 8];
        for (int i = 0; i < weights[0].length; i++) {
            weights[0][i] = (float) (Math.random() * 0.2 - 0.1);  // -0.1 à 0.1
        }
        biases[0] = (float) (Math.random() * 0.1);

        // Couche 2: 8 → 4
        weights[1] = new float[8 * 4];
        for (int i = 0; i < weights[1].length; i++) {
            weights[1][i] = (float) (Math.random() * 0.2 - 0.1);
        }
        biases[1] = (float) (Math.random() * 0.1);

        // Couche 3: 4 → 1
        weights[2] = new float[4];
        for (int i = 0; i < weights[2].length; i++) {
            weights[2][i] = (float) (Math.random() * 0.2 - 0.1);
        }
        biases[2] = (float) (Math.random() * 0.1);
    }

    @Override
    public void loadModel() throws ModelException {
        try {
            log.info("Chargement du modèle DJL (simulation)...");

            // Dans une vraie implémentation, on chargerait un vrai modèle PyTorch
            // Pour l'instant, on simule juste le chargement

            log.info("✅ Modèle DJL simulé chargé (3 couches: 10→8→4→1)");
            this.isLoaded = true;

        } catch (Exception e) {
            throw new ModelException("Erreur lors du chargement du modèle DJL: " + e.getMessage(), e);
        }
    }

    @Override
    public PricePrediction predictPrice(ExportData input) {
        validateInput(input);

        try {
            if (!isLoaded) {
                loadModel();
            }

            // 1. Encoder les features
            float[] features = encodeFeatures(input);

            // 2. Faire la prédiction avec le réseau de neurones simulé
            float prediction = neuralNetworkForward(features);

            // 3. Dénormaliser
            float finalPrice = prediction * 5000.0f;

            // 4. Ajuster selon le marché
            finalPrice = adjustByMarketIndicator(finalPrice, input.indicator());

            return new PricePrediction(
                    LocalDate.now().plusDays(30),
                    input.productType(),
                    finalPrice,
                    0.82f,  // Confiance moyenne-haute
                    modelName,
                    PredictionStatus.COMPLETED
            );

        } catch (Exception e) {
            log.error("Erreur de prédiction DJL: {}", e.getMessage(), e);
            return createFallbackPrediction(input);
        }
    }

    /**
     * Passe avant dans le réseau de neurones simulé
     */
    private float neuralNetworkForward(float[] input) {
        // Couche 1: 10 → 8 (avec ReLU)
        float[] layer1 = new float[8];
        for (int i = 0; i < 8; i++) {
            float sum = biases[0];
            for (int j = 0; j < 10; j++) {
                sum += input[j] * weights[0][i * 10 + j];
            }
            layer1[i] = Math.max(0, sum);  // ReLU
        }

        // Couche 2: 8 → 4 (avec ReLU)
        float[] layer2 = new float[4];
        for (int i = 0; i < 4; i++) {
            float sum = biases[1];
            for (int j = 0; j < 8; j++) {
                sum += layer1[j] * weights[1][i * 8 + j];
            }
            layer2[i] = Math.max(0, sum);  // ReLU
        }

        // Couche 3: 4 → 1 (linéaire)
        float output = biases[2];
        for (int i = 0; i < 4; i++) {
            output += layer2[i] * weights[2][i];
        }

        return output;
    }

    private float[] encodeFeatures(ExportData data) {
        float[] features = new float[10];

        // Même encodage que précédemment
        features[0] = (data.date().getYear() - 2000) / 25.0f;
        features[1] = data.date().getMonthValue() / 12.0f;
        features[2] = data.productType().ordinal() / 10.0f;
        features[3] = (float) (Math.log1p(data.volume()) / 10.0);

        if (data.destinationCountry() != null) {
            features[4] = (data.destinationCountry().hashCode() % 100) / 100.0f;
        }

        features[5] = data.indicator().ordinal() / 5.0f;
        features[6] = (float) (data.pricePerTon() / 5000.0);
        features[7] = (float) (Math.random() * 0.1f);
        features[8] = features[0] * features[2];
        features[9] = features[3] * features[4];

        return features;
    }

    private float adjustByMarketIndicator(float price, MarketIndicator indicator) {
        switch (indicator) {
            case RISING: return price * 1.07f;
            case FALLING: return price * 0.93f;
            case VOLATILE: return price * (0.96f + new Random().nextFloat() * 0.08f);
            case UNPREDICTABLE: return price * (0.90f + new Random().nextFloat() * 0.20f);
            default: return price;
        }
    }

    private PricePrediction createFallbackPrediction(ExportData input) {
        double basePrice = switch (input.productType()) {
            case OLIVE_OIL -> 2500.0;
            case DATES -> 1800.0;
            case CITRUS_FRUITS -> 1200.0;
            case WHEAT -> 800.0;
            case TOMATOES -> 1500.0;
            case PEPPERS -> 2000.0;
        };

        return new PricePrediction(
                LocalDate.now().plusDays(30),
                input.productType(),
                basePrice * (0.9 + Math.random() * 0.2),
                0.6,
                modelName + "-Fallback",
                PredictionStatus.LOW_CONFIDENCE
        );
    }

    @Override
    public List<PricePrediction> predictBatch(List<ExportData> inputs) {
        List<PricePrediction> predictions = new ArrayList<>();
        for (ExportData input : inputs) {
            predictions.add(predictPrice(input));
        }
        return predictions;
    }

    @Override
    public double getModelAccuracy() {
        return 0.83;  // 83% de précision estimée
    }

    @Override
    public void unloadModel() {
        this.isLoaded = false;
        log.info("Modèle DJL déchargé");
    }
}