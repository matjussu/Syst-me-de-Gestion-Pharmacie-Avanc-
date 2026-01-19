package com.sgpa.exception;

/**
 * Exception personnalisee pour les erreurs de la couche d'acces aux donnees (DAO).
 * <p>
 * Cette exception encapsule les erreurs SQL et les problemes de connexion
 * a la base de donnees. Elle permet de separer les exceptions techniques
 * des exceptions metier.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class DAOException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Code d'erreur optionnel pour categoriser l'exception.
     */
    private String errorCode;

    /**
     * Construit une nouvelle DAOException avec un message.
     *
     * @param message le message d'erreur
     */
    public DAOException(String message) {
        super(message);
    }

    /**
     * Construit une nouvelle DAOException avec un message et une cause.
     *
     * @param message le message d'erreur
     * @param cause   la cause originale de l'exception
     */
    public DAOException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construit une nouvelle DAOException avec un message, une cause et un code d'erreur.
     *
     * @param message   le message d'erreur
     * @param cause     la cause originale de l'exception
     * @param errorCode le code d'erreur
     */
    public DAOException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Retourne le code d'erreur associe a cette exception.
     *
     * @return le code d'erreur, ou null si non defini
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DAOException");
        if (errorCode != null) {
            sb.append(" [").append(errorCode).append("]");
        }
        sb.append(": ").append(getMessage());
        return sb.toString();
    }
}
