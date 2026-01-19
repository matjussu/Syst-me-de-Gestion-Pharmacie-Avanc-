package com.sgpa.utils;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilitaire pour le hashage et la verification des mots de passe.
 * <p>
 * Utilise l'algorithme BCrypt qui offre :
 * <ul>
 *   <li>Salage automatique (chaque hash est unique)</li>
 *   <li>Resistance aux attaques par force brute (cout configurable)</li>
 *   <li>Resistance aux rainbow tables</li>
 * </ul>
 * </p>
 *
 * <h3>Utilisation :</h3>
 * <pre>{@code
 * // Hashage d'un nouveau mot de passe
 * String hash = PasswordUtils.hashPassword("monMotDePasse");
 *
 * // Verification lors de la connexion
 * boolean valid = PasswordUtils.verifyPassword("monMotDePasse", hash);
 * }</pre>
 *
 * @author SGPA Team
 * @version 1.0
 */
public final class PasswordUtils {

    private static final Logger logger = LoggerFactory.getLogger(PasswordUtils.class);

    /**
     * Cout du hashage BCrypt (facteur de travail).
     * Une valeur de 10 est un bon compromis securite/performance.
     * Chaque increment double le temps de calcul.
     */
    private static final int BCRYPT_COST = 10;

    /**
     * Longueur minimale d'un mot de passe.
     */
    private static final int MIN_PASSWORD_LENGTH = 4;

    /**
     * Longueur maximale d'un mot de passe (limite BCrypt : 72 caracteres).
     */
    private static final int MAX_PASSWORD_LENGTH = 72;

    /**
     * Constructeur prive - classe utilitaire.
     */
    private PasswordUtils() {
        throw new UnsupportedOperationException("Classe utilitaire - instanciation interdite");
    }

    /**
     * Hash un mot de passe en clair avec BCrypt.
     * <p>
     * Genere automatiquement un sel unique pour chaque hash.
     * </p>
     *
     * @param plainPassword le mot de passe en clair
     * @return le hash BCrypt du mot de passe
     * @throws IllegalArgumentException si le mot de passe est null, vide ou trop court
     */
    public static String hashPassword(String plainPassword) {
        validatePassword(plainPassword);
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
        logger.debug("Mot de passe hashe avec succes");
        return hash;
    }

    /**
     * Verifie si un mot de passe en clair correspond a un hash BCrypt.
     *
     * @param plainPassword  le mot de passe en clair a verifier
     * @param hashedPassword le hash BCrypt a comparer
     * @return true si le mot de passe correspond au hash
     * @throws IllegalArgumentException si le mot de passe ou le hash est null/vide
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            logger.warn("Tentative de verification avec mot de passe vide");
            return false;
        }
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            logger.warn("Tentative de verification avec hash vide");
            return false;
        }

        try {
            boolean matches = BCrypt.checkpw(plainPassword, hashedPassword);
            logger.debug("Verification mot de passe: {}", matches ? "SUCCES" : "ECHEC");
            return matches;
        } catch (IllegalArgumentException e) {
            logger.error("Hash BCrypt invalide", e);
            return false;
        }
    }

    /**
     * Valide un mot de passe selon les regles de securite.
     *
     * @param password le mot de passe a valider
     * @throws IllegalArgumentException si le mot de passe ne respecte pas les regles
     */
    public static void validatePassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas etre null");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Le mot de passe doit contenir au moins " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Le mot de passe ne peut pas depasser " + MAX_PASSWORD_LENGTH + " caracteres");
        }
    }

    /**
     * Verifie si un mot de passe respecte les criteres de complexite.
     * <p>
     * Criteres verifies :
     * <ul>
     *   <li>Au moins une lettre minuscule</li>
     *   <li>Au moins une lettre majuscule</li>
     *   <li>Au moins un chiffre</li>
     *   <li>Longueur minimale de 8 caracteres</li>
     * </ul>
     * </p>
     *
     * @param password le mot de passe a verifier
     * @return true si le mot de passe est suffisamment complexe
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }

        return hasLower && hasUpper && hasDigit;
    }

    /**
     * Genere un sel BCrypt.
     * <p>
     * Note : Cette methode est fournie pour les tests.
     * En production, utilisez directement {@link #hashPassword(String)}.
     * </p>
     *
     * @return un sel BCrypt
     */
    public static String generateSalt() {
        return BCrypt.gensalt(BCRYPT_COST);
    }
}
