package com.sgpa.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gestionnaire de connexion a la base de donnees avec pool de connexions HikariCP.
 * <p>
 * Implementation du pattern Singleton pour garantir une instance unique
 * du pool de connexions dans toute l'application.
 * </p>
 * <p>
 * Configuration chargee depuis le fichier {@code database.properties} situe
 * dans le classpath.
 * </p>
 *
 * <h3>Utilisation :</h3>
 * <pre>{@code
 * try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
 *     // Utiliser la connexion
 * }
 * }</pre>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    /** Nom du fichier de configuration */
    private static final String CONFIG_FILE = "database.properties";

    /** Instance unique (Singleton) */
    private static volatile DatabaseConnection instance;

    /** Pool de connexions HikariCP */
    private final HikariDataSource dataSource;

    /**
     * Constructeur prive - initialise le pool de connexions.
     *
     * @throws RuntimeException si la configuration echoue
     */
    private DatabaseConnection() {
        try {
            Properties props = loadProperties();
            HikariConfig config = createHikariConfig(props);
            this.dataSource = new HikariDataSource(config);
            logger.info("Pool de connexions HikariCP initialise avec succes");
        } catch (Exception e) {
            logger.error("Erreur lors de l'initialisation du pool de connexions", e);
            throw new RuntimeException("Impossible d'initialiser la connexion a la base de donnees", e);
        }
    }

    /**
     * Retourne l'instance unique du gestionnaire de connexion.
     * <p>
     * Implementation thread-safe avec double-checked locking.
     * </p>
     *
     * @return l'instance unique de DatabaseConnection
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Obtient une connexion depuis le pool.
     * <p>
     * La connexion doit etre fermee apres utilisation (idealement avec try-with-resources)
     * pour etre retournee au pool.
     * </p>
     *
     * @return une connexion a la base de donnees
     * @throws SQLException si aucune connexion n'est disponible
     */
    public Connection getConnection() throws SQLException {
        Connection conn = dataSource.getConnection();
        logger.debug("Connexion obtenue du pool (actives: {}, idle: {})",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections());
        return conn;
    }

    /**
     * Ferme le pool de connexions.
     * <p>
     * Doit etre appele lors de l'arret de l'application.
     * </p>
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Pool de connexions HikariCP ferme");
        }
    }

    /**
     * Verifie si le pool est actif et fonctionnel.
     *
     * @return true si le pool est actif
     */
    public boolean isPoolActive() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Retourne des statistiques sur le pool de connexions.
     *
     * @return une chaine contenant les statistiques du pool
     */
    public String getPoolStats() {
        if (dataSource == null || dataSource.isClosed()) {
            return "Pool ferme";
        }
        return String.format("Connexions - Actives: %d, Idle: %d, En attente: %d, Total: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(),
                dataSource.getHikariPoolMXBean().getTotalConnections());
    }

    /**
     * Charge les proprietes depuis le fichier de configuration.
     *
     * @return les proprietes chargees
     * @throws IOException si le fichier n'est pas trouve
     */
    private Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new IOException("Fichier de configuration non trouve: " + CONFIG_FILE);
            }
            props.load(is);
            logger.debug("Configuration chargee depuis {}", CONFIG_FILE);
        }
        return props;
    }

    /**
     * Cree la configuration HikariCP a partir des proprietes.
     *
     * @param props les proprietes de configuration
     * @return la configuration HikariCP
     */
    private HikariConfig createHikariConfig(Properties props) {
        HikariConfig config = new HikariConfig();

        // Configuration de base
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.username"));
        config.setPassword(props.getProperty("db.password", ""));

        // Configuration du pool
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.size", "10")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "5")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "300000")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "20000")));
        config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetime", "1200000")));

        // Configuration MySQL optimisee
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        // Nom du pool pour le monitoring
        config.setPoolName("SGPA-HikariPool");

        logger.info("Configuration HikariCP: URL={}, Pool Size={}",
                config.getJdbcUrl(), config.getMaximumPoolSize());

        return config;
    }

    /**
     * Teste la connexion a la base de donnees.
     *
     * @return true si la connexion est valide
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean valid = conn.isValid(5);
            logger.info("Test de connexion: {}", valid ? "SUCCES" : "ECHEC");
            return valid;
        } catch (SQLException e) {
            logger.error("Test de connexion echoue", e);
            return false;
        }
    }
}
