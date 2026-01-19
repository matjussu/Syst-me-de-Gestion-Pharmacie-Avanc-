package com.sgpa.exception;

/**
 * Exception personnalisee pour les erreurs de la couche service (logique metier).
 * <p>
 * Cette exception est utilisee pour signaler les violations des regles metier
 * et les erreurs de validation. Elle permet de separer les exceptions metier
 * des exceptions techniques.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class ServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Code d'erreur pour categoriser l'exception.
     */
    private String errorCode;

    /**
     * Type d'erreur metier.
     */
    private ErrorType errorType;

    /**
     * Types d'erreurs metier possibles.
     */
    public enum ErrorType {
        /** Erreur de validation des donnees */
        VALIDATION,
        /** Stock insuffisant */
        STOCK_INSUFFISANT,
        /** Medicament perime */
        MEDICAMENT_PERIME,
        /** Ordonnance requise */
        ORDONNANCE_REQUISE,
        /** Utilisateur non autorise */
        NON_AUTORISE,
        /** Entite non trouvee */
        NOT_FOUND,
        /** Erreur generique */
        GENERIC
    }

    /**
     * Construit une nouvelle ServiceException avec un message.
     *
     * @param message le message d'erreur
     */
    public ServiceException(String message) {
        super(message);
        this.errorType = ErrorType.GENERIC;
    }

    /**
     * Construit une nouvelle ServiceException avec un message et un type.
     *
     * @param message   le message d'erreur
     * @param errorType le type d'erreur metier
     */
    public ServiceException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    /**
     * Construit une nouvelle ServiceException avec un message et une cause.
     *
     * @param message le message d'erreur
     * @param cause   la cause originale de l'exception
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.GENERIC;
    }

    /**
     * Construit une nouvelle ServiceException avec un message, un type et une cause.
     *
     * @param message   le message d'erreur
     * @param errorType le type d'erreur metier
     * @param cause     la cause originale de l'exception
     */
    public ServiceException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    /**
     * Retourne le code d'erreur.
     *
     * @return le code d'erreur
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Definit le code d'erreur.
     *
     * @param errorCode le code d'erreur
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Retourne le type d'erreur metier.
     *
     * @return le type d'erreur
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * Definit le type d'erreur metier.
     *
     * @param errorType le type d'erreur
     */
    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ServiceException");
        if (errorType != null) {
            sb.append(" [").append(errorType).append("]");
        }
        if (errorCode != null) {
            sb.append(" (").append(errorCode).append(")");
        }
        sb.append(": ").append(getMessage());
        return sb.toString();
    }
}
