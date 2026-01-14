package tn.sesame.economics.model;

/**
 * Énumération représentant les types de produits agricoles tunisiens.
 * Chaque constante contient son nom en français pour l'affichage.
 *
 * @since Java 25
 */
public enum ProductType {

    /**
     * Huile d'olive - Produit phare de l'exportation tunisienne
     */
    OLIVE_OIL("Huile d'olive"),

    /**
     * Dattes - Produit traditionnel de qualité
     */
    DATES("Dattes"),

    /**
     * Agrumes - Oranges, clémentines, citrons
     */
    CITRUS_FRUITS("Agrumes"),

    /**
     * Blé - Céréale de base pour l'exportation
     */
    WHEAT("Blé"),

    /**
     * Tomates - Produit frais ou transformé
     */
    TOMATOES("Tomates"),

    /**
     * Piments - Produit épicé, souvent transformé
     */
    PEPPERS("Piments");

    private final String frenchName;

    /**
     * Constructeur privé de l'énumération.
     *
     * @param frenchName Le nom français du produit agricole
     */
    private ProductType(String frenchName) {
        this.frenchName = frenchName;
    }

    /**
     * Retourne le nom français du produit agricole.
     *
     * @return Le nom en français
     */
    public String getFrenchName() {
        return frenchName;
    }

    /**
     * Retourne la constante correspondant au nom français.
     *
     * @param frenchName Le nom français du produit
     * @return La constante ProductType correspondante
     * @throws IllegalArgumentException si le nom n'est pas trouvé
     */
    public static ProductType fromFrenchName(String frenchName) {
        for (ProductType type : values()) {
            if (type.getFrenchName().equalsIgnoreCase(frenchName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type de produit non trouvé: " + frenchName);
    }

    /**
     * Retourne la représentation sous forme de chaîne de l'énumération.
     * Utilise le nom français pour une meilleure lisibilité.
     *
     * @return Le nom français du produit
     */
    @Override
    public String toString() {
        return frenchName;
    }
}