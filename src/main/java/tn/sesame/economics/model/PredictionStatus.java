package tn.sesame.economics.model;

/**
 * Énumération représentant l'état d'une prédiction de prix générée par le modèle d'IA.
 * Utilisée pour suivre le cycle de vie et la fiabilité des prédictions.
 *
 * @since Java 25
 */
public enum PredictionStatus {

    /**
     * Prédiction en attente de traitement
     */
    PENDING,

    /**
     * Prédiction complétée avec succès
     */
    COMPLETED,

    /**
     * Prédiction échouée (erreur technique)
     */
    FAILED,

    /**
     * Prédiction avec faible score de confiance
     */
    LOW_CONFIDENCE;

    /**
     * Vérifie si le statut indique une prédiction terminée.
     *
     * @return true si la prédiction est dans un état final
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED;
    }

    /**
     * Vérifie si le statut indique une prédiction valide.
     *
     * @return true si la prédiction peut être utilisée pour l'analyse
     */
    public boolean isValid() {
        return this == COMPLETED || this == LOW_CONFIDENCE;
    }

    /**
     * Vérifie si le statut indique un problème technique.
     *
     * @return true si la prédiction a échoué
     */
    public boolean hasError() {
        return this == FAILED;
    }

    /**
     * Vérifie si le statut nécessite une attention particulière.
     *
     * @return true si la prédiction nécessite une revue manuelle
     */
    public boolean requiresAttention() {
        return this == LOW_CONFIDENCE || this == PENDING;
    }

    /**
     * Retourne la description du statut en français.
     *
     * @return Description textuelle du statut
     */
    public String getDescription() {
        return switch (this) {
            case PENDING -> "Prédiction en attente de traitement";
            case COMPLETED -> "Prédiction générée avec succès";
            case FAILED -> "Échec de la génération de prédiction";
            case LOW_CONFIDENCE -> "Prédiction générée avec faible confiance";
        };
    }

    /**
     * Retourne le symbole représentant le statut.
     *
     * @return Symbole emoji représentatif
     */
    public String getSymbol() {
        return switch (this) {
            case PENDING -> "⏳";
            case COMPLETED -> "✅";
            case FAILED -> "❌";
            case LOW_CONFIDENCE -> "⚠️";
        };
    }

    /**
     * Retourne la couleur associée au statut pour l'interface utilisateur.
     *
     * @return Nom de la couleur
     */
    public String getColor() {
        return switch (this) {
            case PENDING -> "orange";
            case COMPLETED -> "green";
            case FAILED -> "red";
            case LOW_CONFIDENCE -> "yellow";
        };
    }

    /**
     * Retourne la priorité de traitement du statut.
     *
     * @return Niveau de priorité (1=urgent, 5=non urgent)
     */
    public int getPriority() {
        return switch (this) {
            case FAILED -> 1;       // Urgent - nécessite intervention
            case LOW_CONFIDENCE -> 2; // Élevé - nécessite vérification
            case PENDING -> 3;      // Moyen - en attente de traitement
            case COMPLETED -> 5;    // Faible - déjà traité
        };
    }

    /**
     * Retourne la représentation formatée du statut avec symbole.
     *
     * @return Chaîne formatée "symbole nom"
     */
    @Override
    public String toString() {
        return getSymbol() + " " + name();
    }
}