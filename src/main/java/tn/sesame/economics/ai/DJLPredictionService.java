package tn.sesame.economics.ai;

import tn.sesame.economics.annotation.AIService;
import tn.sesame.economics.exception.ModelException;
import tn.sesame.economics.model.ExportData;
import tn.sesame.economics.model.PricePrediction;
import tn.sesame.economics.model.PredictionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * DJL-based prediction service for Tunisian export price prediction.
 * Simplified wrapper around DJLRealModel.
 */
@AIService(provider = "DJL", version = "0.28.0")  // Fixed version to match pom.xml
public class DJLPredictionService extends BaseAIModel {

    private static final Logger LOGGER = Logger.getLogger(DJLPredictionService.class.getName());

    private DJLRealModel realModel;

    /**
     * Constructor
     */
    public DJLPredictionService() {
        super("DJL-Price-Predictor");
        this.realModel = new DJLRealModel();
    }

    /**
     * Load the underlying DJL model
     */
    @Override
    public void loadModel() throws ModelException {
        try {
            LOGGER.info("Loading DJL prediction service...");
            realModel.loadModel();
            this.isLoaded = true;
            LOGGER.info("DJL prediction service loaded successfully");
        } catch (Exception e) {
            throw new ModelException("Failed to load DJL prediction service", e);
        }
    }

    /**
     * Predict price for single export data
     */
    @Override
    public PricePrediction predictPrice(ExportData input) {
        validateInput(input);

        if (!isLoaded) {
            LOGGER.warning("Model not loaded, attempting to load...");
            try {
                loadModel();
            } catch (ModelException e) {
                LOGGER.severe("Failed to load model: " + e.getMessage());
                return PricePrediction.failedPrediction(
                        LocalDate.now(),
                        input.productType(),
                        modelName
                );
            }
        }

        return realModel.predictPrice(input);
    }

    /**
     * Batch prediction
     */
    @Override
    public List<PricePrediction> predictBatch(List<ExportData> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("Input list cannot be null or empty");
        }

        validateBatchInput(inputs);

        if (!isLoaded) {
            LOGGER.warning("Model not loaded, attempting to load...");
            try {
                loadModel();
            } catch (ModelException e) {
                LOGGER.severe("Failed to load model: " + e.getMessage());
                // Return failed predictions for all inputs
                return inputs.stream()
                        .map(input -> PricePrediction.failedPrediction(
                                LocalDate.now(),
                                input.productType(),
                                modelName
                        ))
                        .toList();
            }
        }

        return realModel.predictBatch(inputs);
    }

    /**
     * Validate batch input
     */
    private void validateBatchInput(List<ExportData> inputs) {
        if (inputs == null) {
            throw new IllegalArgumentException("Input list cannot be null");
        }
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Input list cannot be empty");
        }
        // Validate each input
        for (ExportData input : inputs) {
            validateInput(input);
        }
    }

    /**
     * Get model accuracy from real model
     */
    @Override
    public double getModelAccuracy() {
        if (isLoaded) {
            return realModel.getModelAccuracy();
        }
        return super.getModelAccuracy();
    }

    /**
     * Unload the model
     */
    @Override
    public void unloadModel() throws ModelException {
        try {
            realModel.unloadModel();
            this.isLoaded = false;
            LOGGER.info("DJL prediction service unloaded");
        } catch (Exception e) {
            throw new ModelException("Failed to unload DJL prediction service", e);
        }
    }

    /**
     * Get detailed model information
     */
    public String getModelInfo() {
        if (isLoaded) {
            return String.format(
                    "DJLPredictionService{model=%s, loaded=%s, accuracy=%.4f}",
                    modelName,
                    isLoaded,
                    getModelAccuracy()
            );
        }
        return String.format("DJLPredictionService{model=%s, loaded=%s}", modelName, isLoaded);
    }

    /**
     * Get the underlying real model for advanced operations
     */
    public DJLRealModel getRealModel() {
        return realModel;
    }

    /**
     * Retrain the model with new data
     */
    public void retrainModel(List<ExportData> newData) throws ModelException {
        if (!isLoaded) {
            throw new IllegalStateException("Model must be loaded before retraining");
        }
        try {
            realModel.retrainModel(newData);
        } catch (Exception e) {
            throw new ModelException("Failed to retrain model", e);
        }
    }

    /**
     * Get training metrics
     */
    public String getTrainingMetrics() {
        if (isLoaded) {
            return realModel.getTrainingMetrics();
        }
        return "Model not loaded";
    }

    /**
     * Train the model if not already trained
     */
    public void trainModel() throws ModelException {
        if (!isLoaded) {
            loadModel();
        }
        realModel.trainModel();
    }
}