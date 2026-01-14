package tn.sesame.economics.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour valider les méthodes de prédiction des modèles d'IA.
 * Cette annotation permet de spécifier les critères de validation minimum
 * pour les résultats des modèles de prédiction.
 *
 * @since Java 25
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelValidation {

    /**
     * Score de confiance minimum requis pour que la prédiction soit considérée comme valide.
     * Les valeurs doivent être comprises entre 0.0 et 1.0.
     *
     * @return Le score de confiance minimum (par défaut 0.5)
     */
    double minConfidence() default 0.5;

    /**
     * Description optionnelle expliquant les critères de validation.
     * Utile pour la documentation et les messages d'erreur.
     *
     * @return La description de la validation
     */
    String description() default "";
}