package tn.sesame.economics.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour marquer les classes de service d'IA.
 * Cette annotation fournit des métadonnées sur le fournisseur et la version
 * du service d'intelligence artificielle.
 *
 * @since Java 25
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AIService {

    /**
     * Nom du fournisseur du service d'IA.
     * Exemples: "DJL", "TensorFlow", "ONNX Runtime", "Custom ML"
     *
     * @return Le nom du fournisseur (obligatoire)
     */
    String provider();

    /**
     * Version du service ou du modèle d'IA.
     * Doit suivre le format sémantique (ex: "1.2.0").
     *
     * @return La version du service (par défaut "1.0")
     */
    String version() default "1.0";
}