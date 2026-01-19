package com.sgpa.dao.impl;

import com.sgpa.dao.ConsommationDAO;
import com.sgpa.exception.DAOException;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation JDBC de l'interface {@link ConsommationDAO}.
 * <p>
 * Fournit les methodes pour analyser l'historique des ventes et calculer
 * les statistiques de consommation pour les predictions de reapprovisionnement.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class ConsommationDAOImpl implements ConsommationDAO {

    private static final Logger logger = LoggerFactory.getLogger(ConsommationDAOImpl.class);

    // Consommation totale sur une periode
    private static final String SQL_CONSOMMATION_PERIODE =
            "SELECT COALESCE(SUM(lv.quantite), 0) AS total " +
            "FROM ligne_ventes lv " +
            "JOIN lots l ON lv.id_lot = l.id_lot " +
            "JOIN ventes v ON lv.id_vente = v.id_vente " +
            "WHERE l.id_medicament = ? " +
            "AND DATE(v.date_vente) BETWEEN ? AND ?";

    // Historique des ventes journalieres
    private static final String SQL_HISTORIQUE_CONSOMMATION =
            "SELECT DATE(v.date_vente) AS date_vente, SUM(lv.quantite) AS quantite_totale " +
            "FROM ventes v " +
            "JOIN ligne_ventes lv ON v.id_vente = lv.id_vente " +
            "JOIN lots l ON lv.id_lot = l.id_lot " +
            "WHERE l.id_medicament = ? " +
            "AND v.date_vente >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
            "GROUP BY DATE(v.date_vente) " +
            "ORDER BY date_vente";

    // Consommation moyenne journaliere
    private static final String SQL_CONSOMMATION_MOYENNE =
            "SELECT COALESCE(SUM(lv.quantite), 0) / ? AS conso_moyenne " +
            "FROM ligne_ventes lv " +
            "JOIN lots l ON lv.id_lot = l.id_lot " +
            "JOIN ventes v ON lv.id_vente = v.id_vente " +
            "WHERE l.id_medicament = ? " +
            "AND v.date_vente >= DATE_SUB(CURDATE(), INTERVAL ? DAY)";

    // Stock total d'un medicament
    private static final String SQL_STOCK_TOTAL =
            "SELECT COALESCE(SUM(quantite_stock), 0) FROM lots WHERE id_medicament = ?";

    // Stock vendable (non perime) d'un medicament
    private static final String SQL_STOCK_VENDABLE =
            "SELECT COALESCE(SUM(quantite_stock), 0) FROM lots " +
            "WHERE id_medicament = ? AND date_peremption >= CURDATE() AND quantite_stock > 0";

    // Statistiques globales pour tous les medicaments actifs
    private static final String SQL_STATISTIQUES_GLOBALES =
            "SELECT " +
            "    m.id_medicament, " +
            "    m.nom_commercial, " +
            "    m.seuil_min, " +
            "    COALESCE(stock_total.total, 0) AS stock_total, " +
            "    COALESCE(stock_vendable.total, 0) AS stock_vendable, " +
            "    COALESCE(conso.total, 0) AS consommation_periode, " +
            "    COALESCE(conso.total, 0) / ? AS consommation_moyenne_jour " +
            "FROM medicaments m " +
            "LEFT JOIN ( " +
            "    SELECT id_medicament, SUM(quantite_stock) AS total " +
            "    FROM lots GROUP BY id_medicament " +
            ") stock_total ON m.id_medicament = stock_total.id_medicament " +
            "LEFT JOIN ( " +
            "    SELECT id_medicament, SUM(quantite_stock) AS total " +
            "    FROM lots WHERE date_peremption >= CURDATE() AND quantite_stock > 0 " +
            "    GROUP BY id_medicament " +
            ") stock_vendable ON m.id_medicament = stock_vendable.id_medicament " +
            "LEFT JOIN ( " +
            "    SELECT l.id_medicament, SUM(lv.quantite) AS total " +
            "    FROM ligne_ventes lv " +
            "    JOIN lots l ON lv.id_lot = l.id_lot " +
            "    JOIN ventes v ON lv.id_vente = v.id_vente " +
            "    WHERE v.date_vente >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
            "    GROUP BY l.id_medicament " +
            ") conso ON m.id_medicament = conso.id_medicament " +
            "WHERE m.actif = TRUE " +
            "ORDER BY m.nom_commercial";

    @Override
    public int getConsommationByPeriode(int idMedicament, LocalDate dateDebut, LocalDate dateFin) throws DAOException {
        logger.debug("Calcul consommation medicament {} entre {} et {}", idMedicament, dateDebut, dateFin);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_CONSOMMATION_PERIODE)) {

            ps.setInt(1, idMedicament);
            ps.setDate(2, Date.valueOf(dateDebut));
            ps.setDate(3, Date.valueOf(dateFin));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    logger.debug("Consommation medicament {}: {} unites", idMedicament, total);
                    return total;
                }
            }
            return 0;

        } catch (SQLException e) {
            logger.error("Erreur lors du calcul de la consommation", e);
            throw new DAOException("Erreur lors du calcul de la consommation", e);
        }
    }

    @Override
    public Map<LocalDate, Integer> getHistoriqueConsommation(int idMedicament, int nbJours) throws DAOException {
        logger.debug("Recuperation historique consommation medicament {} sur {} jours", idMedicament, nbJours);
        Map<LocalDate, Integer> historique = new HashMap<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_HISTORIQUE_CONSOMMATION)) {

            ps.setInt(1, idMedicament);
            ps.setInt(2, nbJours);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("date_vente").toLocalDate();
                    int quantite = rs.getInt("quantite_totale");
                    historique.put(date, quantite);
                }
            }
            logger.debug("Historique recupere: {} jours avec ventes", historique.size());
            return historique;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recuperation de l'historique", e);
            throw new DAOException("Erreur lors de la recuperation de l'historique", e);
        }
    }

    @Override
    public double getConsommationMoyenneJournaliere(int idMedicament, int nbJours) throws DAOException {
        logger.debug("Calcul consommation moyenne journaliere medicament {} sur {} jours", idMedicament, nbJours);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_CONSOMMATION_MOYENNE)) {

            ps.setInt(1, nbJours);
            ps.setInt(2, idMedicament);
            ps.setInt(3, nbJours);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double moyenne = rs.getDouble("conso_moyenne");
                    logger.debug("Consommation moyenne medicament {}: {} unites/jour", idMedicament, moyenne);
                    return moyenne;
                }
            }
            return 0.0;

        } catch (SQLException e) {
            logger.error("Erreur lors du calcul de la consommation moyenne", e);
            throw new DAOException("Erreur lors du calcul de la consommation moyenne", e);
        }
    }

    @Override
    public List<StatConsommation> getStatistiquesConsommation(int nbJours) throws DAOException {
        logger.debug("Recuperation statistiques consommation sur {} jours", nbJours);
        List<StatConsommation> stats = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_STATISTIQUES_GLOBALES)) {

            ps.setInt(1, nbJours);
            ps.setInt(2, nbJours);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StatConsommation stat = new StatConsommation(
                            rs.getInt("id_medicament"),
                            rs.getString("nom_commercial"),
                            rs.getInt("seuil_min"),
                            rs.getInt("stock_total"),
                            rs.getInt("stock_vendable"),
                            rs.getInt("consommation_periode"),
                            rs.getDouble("consommation_moyenne_jour")
                    );
                    stats.add(stat);
                }
            }
            logger.info("{} statistiques de medicaments recuperees", stats.size());
            return stats;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recuperation des statistiques", e);
            throw new DAOException("Erreur lors de la recuperation des statistiques", e);
        }
    }

    @Override
    public int getStockTotal(int idMedicament) throws DAOException {
        logger.debug("Calcul stock total medicament {}", idMedicament);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_STOCK_TOTAL)) {

            ps.setInt(1, idMedicament);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            logger.error("Erreur lors du calcul du stock total", e);
            throw new DAOException("Erreur lors du calcul du stock total", e);
        }
    }

    @Override
    public int getStockVendable(int idMedicament) throws DAOException {
        logger.debug("Calcul stock vendable medicament {}", idMedicament);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_STOCK_VENDABLE)) {

            ps.setInt(1, idMedicament);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            logger.error("Erreur lors du calcul du stock vendable", e);
            throw new DAOException("Erreur lors du calcul du stock vendable", e);
        }
    }
}
