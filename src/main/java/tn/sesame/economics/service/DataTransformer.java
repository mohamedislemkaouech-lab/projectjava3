package tn.sesame.economics.service;

/**
 * Interface fonctionnelle définissant une transformation de données.
 * Cette interface permet de créer des pipelines de transformation de données
 * pour le prétraitement des données d'exportation.
 *
 * @param <T> Le type de données d'entrée
 * @param <R> Le type de données de sortie
 *
 * @since Java 25
 */
@FunctionalInterface
public interface DataTransformer<T, R> {

    /**
     * Transforme les données d'entrée en données de sortie.
     *
     * @param input Les données à transformer
     * @return Les données transformées
     * @throws IllegalArgumentException si les données d'entrée sont invalides
     */
    R transform(T input);

    /**
     * Compose cette transformation avec une autre transformation.
     * Permet de créer des pipelines de transformation séquentiels.
     *
     * @param after La transformation à appliquer après celle-ci
     * @return Une nouvelle transformation qui applique cette transformation puis la suivante
     * @throws NullPointerException si 'after' est null
     */
    default DataTransformer<T, R> andThen(DataTransformer<R, R> after) {
        return (T t) -> after.transform(transform(t));
    }

    /**
     * Crée un DataTransformer qui applique une fonction.
     *
     * @param function La fonction à appliquer
     * @param <T> Le type d'entrée
     * @param <R> Le type de sortie
     * @return Un DataTransformer basé sur la fonction
     * @throws NullPointerException si la fonction est null
     */
    static <T, R> DataTransformer<T, R> of(java.util.function.Function<T, R> function) {
        return function::apply;
    }

    /**
     * Crée un DataTransformer identité qui retourne l'entrée sans modification.
     *
     * @param <T> Le type de données
     * @return Un DataTransformer identité
     */
    static <T> DataTransformer<T, T> identity() {
        return t -> t;
    }
}