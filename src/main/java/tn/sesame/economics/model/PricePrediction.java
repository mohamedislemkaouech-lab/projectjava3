package tn.sesame.economics.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Record immuable représentant une prédiction de prix générée par un modèle d'IA.
 * Ce record encapsule le résultat de l'inférence du modèle avec ses métadonnées.
 *
 * @param predictionDate Date pour laquelle la prédiction est faite
 * @param productType Type de produit agricole concerné
 * @param predictedPrice Prix prédit par le modèle (doit être ≥ 0)
 * @param confidence Score de confiance du modèle (entre 0.0 et 1.0)
 * @param modelName Nom du modèle d'IA utilisé pour la prédiction
 * @param status Statut de la prédiction (succès, échec, etc.)
 *
 * @since Java 25
 */
public record PricePrediction(
        LocalDate predictionDate,
        ProductType productType,
        double predictedPrice,
        double confidence,
        String modelName,
        PredictionStatus status
) {

    /**
     * Constructeur compact avec validation des données d'entrée.
     * Garantit l'intégrité des données de prédiction.
     *
     * @throws IllegalArgumentException si les valeurs sont hors limites
     * @throws NullPointerException si un paramètre non-nullable est null
     */
    public PricePrediction {
        // Validation des champs non-nullables
        Objects.requireNonNull(predictionDate, "La date de prédiction ne peut pas être null");
        Objects.requireNonNull(productType, "Le type de produit ne peut pas être null");
        Objects.requireNonNull(modelName, "Le nom du modèle ne peut pas être null");
        Objects.requireNonNull(status, "Le statut ne peut pas être null");

        // Validation de la chaîne modelName
        if (modelName.isBlank()) {
            throw new IllegalArgumentException("Le nom du modèle ne peut pas être vide");
        }

        // Validation des valeurs numériques
        if (predictedPrice < 0) {
            throw new IllegalArgumentException(
                    String.format("Le prix prédit ne peut pas être négatif: %.2f", predictedPrice)
            );
        }

        // Validation du score de confiance (doit être entre 0 et 1)
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException(
                    String.format("Le score de confiance doit être entre 0.0 et 1.0: %.2f", confidence)
            );
        }

        // Validation métier supplémentaire
        if (predictedPrice > 1_000_000) {
            throw new IllegalArgumentException(
                    String.format("Prix prédit anormalement élevé: %.2f", predictedPrice)
            );
        }

        // Normalisation: trim du nom du modèle
        modelName = modelName.trim();
    }

    /**
     * Vérifie si la prédiction est considérée comme fiable.
     * Une prédiction est fiable si elle est complétée avec un score de confiance élevé.
     *
     * @return true si la prédiction est fiable pour la prise de décision
     */
    public boolean isReliable() {
        return status == PredictionStatus.COMPLETED && confidence >= 0.8;
    }

    /**
     * Vérifie si la prédiction nécessite une vérification manuelle.
     *
     * @return true si la prédiction a un faible score de confiance ou un statut suspect
     */
    public boolean requiresManualVerification() {
        return confidence < 0.6 || status == PredictionStatus.LOW_CONFIDENCE;
    }

    /**
     * Calcule l'intervalle de confiance de la prédiction.
     *
     * @param margin Marge d'erreur (par exemple 0.1 pour 10%)
     * @return Tableau de deux doubles [prix_min, prix_max]
     */
    public double[] getConfidenceInterval(double margin) {
        if (margin < 0 || margin > 1.0) {
            throw new IllegalArgumentException("La marge doit être entre 0.0 et 1.0");
        }

        double range = predictedPrice * margin * confidence;
        return new double[] {
                Math.max(0, predictedPrice - range),
                predictedPrice + range
        };
    }

    /**
     * Retourne le niveau de risque basé sur le score de confiance.
     *
     * @return Niveau de risque (1=faible, 5=élevé)
     */
    public int getRiskLevel() {
        if (confidence >= 0.9) return 1;  // Très faible risque
        if (confidence >= 0.8) return 2;  // Faible risque
        if (confidence >= 0.7) return 3;  // Risque modéré
        if (confidence >= 0.6) return 4;  // Risque élevé
        return 5;                         // Très élevé risque
    }

    /**
     * Retourne une description textuelle du niveau de risque.
     *
     * @return Description du risque
     */
    public String getRiskDescription() {
        return switch (getRiskLevel()) {
            case 1 -> "Très faible risque";
            case 2 -> "Faible risque";
            case 3 -> "Risque modéré";
            case 4 -> "Risque élevé";
            case 5 -> "Très élevé risque";
            default -> "Risque inconnu";
        };
    }

    /**
     * Crée une nouvelle instance avec un statut mis à jour.
     *
     * @param newStatus Le nouveau statut
     * @return Une nouvelle instance avec le statut mis à jour
     */
    public PricePrediction withStatus(PredictionStatus newStatus) {
        return new PricePrediction(
                predictionDate,
                productType,
                predictedPrice,
                confidence,
                modelName,
                newStatus
        );
    }

    /**
     * Crée une nouvelle instance avec un score de confiance mis à jour.
     *
     * @param newConfidence Le nouveau score de confiance
     * @return Une nouvelle instance avec le score de confiance mis à jour
     * @throws IllegalArgumentException si le nouveau score n'est pas entre 0.0 et 1.0
     */
    public PricePrediction withConfidence(double newConfidence) {
        return new PricePrediction(
                predictionDate,
                productType,
                predictedPrice,
                newConfidence,
                modelName,
                status
        );
    }

    /**
     * Crée une nouvelle instance avec un prix prédit mis à jour.
     *
     * @param newPrice Le nouveau prix prédit
     * @return Une nouvelle instance avec le prix prédit mis à jour
     * @throws IllegalArgumentException si le nouveau prix est négatif
     */
    public PricePrediction withPredictedPrice(double newPrice) {
        return new PricePrediction(
                predictionDate,
                productType,
                newPrice,
                confidence,
                modelName,
                status
        );
    }

    /**
     * Retourne une représentation formatée de la prédiction.
     *
     * @return Chaîne formatée avec toutes les informations
     */
    @Override
    public String toString() {
        return String.format(
                "PricePrediction[date=%s, produit=%s, prix=%.2f€, confiance=%.2f%%, " +
                        "modèle=%s, statut=%s, risque=%d (%s)]",
                predictionDate,
                productType.getFrenchName(),
                predictedPrice,
                confidence * 100,
                modelName,
                status,
                getRiskLevel(),
                getRiskDescription()
        );
    }

    /**
     * Retourne un résumé simplifié pour l'affichage dans les tableaux.
     *
     * @return Résumé court de la prédiction
     */
    public String getSummary() {
        return String.format("%s: %.2f€ (%.0f%% confiance, risque: %d)",
                productType.getFrenchName(),
                predictedPrice,
                confidence * 100,
                getRiskLevel()
        );
    }

    /**
     * Retourne un format CSV pour l'exportation.
     *
     * @return Ligne CSV formatée
     */
    public String toCSV() {
        return String.format("%s,%s,%.2f,%.2f,%s,%s",
                predictionDate,
                productType,
                predictedPrice,
                confidence,
                modelName,
                status
        );
    }

    /**
     * Compare cette prédiction avec une autre pour vérifier la cohérence.
     *
     * @param other Autre prédiction à comparer
     * @param tolerance Tolérance de différence en pourcentage
     * @return true si les prédictions sont cohérentes dans la tolérance donnée
     */
    public boolean isConsistentWith(PricePrediction other, double tolerance) {
        if (!productType.equals(other.productType)) {
            return false;
        }

        double priceDifference = Math.abs(predictedPrice - other.predictedPrice);
        double avgPrice = (predictedPrice + other.predictedPrice) / 2.0;
        double percentageDifference = (priceDifference / avgPrice) * 100.0;

        return percentageDifference <= tolerance;
    }

    /**
     * Vérifie si cette prédiction est basée sur des données récentes.
     *
     * @param days Nombre maximum de jours depuis aujourd'hui
     * @return true si la prédiction est récente
     */
    public boolean isRecent(int days) {
        LocalDate today = LocalDate.now();
        return predictionDate.isAfter(today.minusDays(days));
    }

    /**
     * Calcule la valeur monétaire totale basée sur un volume donné.
     *
     * @param volume Volume en tonnes
     * @return Valeur totale prédite
     */
    public double calculateTotalValue(double volume) {
        if (volume < 0) {
            throw new IllegalArgumentException("Le volume ne peut pas être négatif");
        }
        return predictedPrice * volume;
    }

    /**
     * Méthode utilitaire pour créer une prédiction avec un statut d'échec.
     * Utile pour la gestion d'erreurs.
     *
     * @param predictionDate Date de la prédiction
     * @param productType Type de produit
     * @param modelName Nom du modèle
     * @return Une prédiction avec statut FAILED
     */
    public static PricePrediction failedPrediction(
            LocalDate predictionDate,
            ProductType productType,
            String modelName
    ) {
        return new PricePrediction(
                predictionDate,
                productType,
                0.0,
                0.0,
                modelName,
                PredictionStatus.FAILED
        );
    }

    /**
     * Méthode utilitaire pour créer une prédiction avec un statut en attente.
     *
     * @param predictionDate Date de la prédiction
     * @param productType Type de produit
     * @param modelName Nom du modèle
     * @return Une prédiction avec statut PENDING
     */
    public static PricePrediction pendingPrediction(
            LocalDate predictionDate,
            ProductType productType,
            String modelName
    ) {
        return new PricePrediction(
                predictionDate,
                productType,
                0.0,
                0.0,
                modelName,
                PredictionStatus.PENDING
        );
    }

    /**
     * Méthode utilitaire pour créer une prédiction de démonstration.
     * Utile pour les tests et présentations.
     *
     * @param date Date de prédiction
     * @param product Produit
     * @param basePrice Prix de base
     * @param confidenceMultiplicateur Multiplicateur de confiance
     * @param model Nom du modèle
     * @return Prédiction de démonstration
     */
    public static PricePrediction demoPrediction(
            LocalDate date,
            ProductType product,
            double basePrice,
            double confidenceMultiplicateur,
            String model
    ) {
        double adjustedPrice = basePrice * (0.9 + Math.random() * 0.2);
        double confidence = Math.min(0.95, 0.7 + Math.random() * 0.3 * confidenceMultiplicateur);

        return new PricePrediction(
                date,
                product,
                adjustedPrice,
                confidence,
                model,
                PredictionStatus.COMPLETED
        );
    }

    /**
     * Compare deux prédictions pour le tri par prix.
     *
     * @param other Autre prédiction à comparer
     * @return Résultat de la comparaison
     */
    public int compareByPrice(PricePrediction other) {
        return Double.compare(this.predictedPrice, other.predictedPrice);
    }

    /**
     * Compare deux prédictions pour le tri par confiance.
     *
     * @param other Autre prédiction à comparer
     * @return Résultat de la comparaison
     */
    public int compareByConfidence(PricePrediction other) {
        return Double.compare(this.confidence, other.confidence);
    }

    /**
     * Compare deux prédictions pour le tri par date.
     *
     * @param other Autre prédiction à comparer
     * @return Résultat de la comparaison
     */
    public int compareByDate(PricePrediction other) {
        return this.predictionDate.compareTo(other.predictionDate);
    }

    /**
     * Retourne une représentation JSON de la prédiction.
     *
     * @return Chaîne JSON
     */
    public String toJSON() {
        return String.format(
                "{\"date\":\"%s\",\"product\":\"%s\",\"price\":%.2f,\"confidence\":%.2f," +
                        "\"model\":\"%s\",\"status\":\"%s\",\"riskLevel\":%d}",
                predictionDate,
                productType,
                predictedPrice,
                confidence,
                modelName,
                status,
                getRiskLevel()
        );
    }

    /**
     * Vérifie si la prédiction est valide pour l'analyse.
     *
     * @return true si la prédiction peut être utilisée dans l'analyse
     */
    public boolean isValidForAnalysis() {
        return status == PredictionStatus.COMPLETED &&
                confidence > 0.5 &&
                predictedPrice > 0;
    }

    /**
     * Retourne une catégorie de prix pour l'analyse.
     *
     * @return Catégorie (bas, moyen, haut)
     */
    public String getPriceCategory() {
        if (predictedPrice < 1000) return "Bas";
        if (predictedPrice < 3000) return "Moyen";
        return "Élevé";
    }

    /**
     * Calcule le rendement potentiel basé sur un coût donné.
     *
     * @param cost Coût de production par tonne
     * @return Rendement en pourcentage
     */
    public double calculateYield(double cost) {
        if (cost <= 0) {
            throw new IllegalArgumentException("Le coût doit être positif");
        }
        return ((predictedPrice - cost) / cost) * 100;
    }
}