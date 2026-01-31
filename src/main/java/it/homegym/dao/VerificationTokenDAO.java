package it.homegym.dao;

import it.homegym.util.ConnectionPool;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.logging.Logger;

public class VerificationTokenDAO {
    private static final Logger LOG = Logger.getLogger(VerificationTokenDAO.class.getName());

    // crea un nuovo token
    public int createToken(int userId, String token, Timestamp expiresAt) throws SQLException {
        String sql = "INSERT INTO verification_token (user_id, token, expires_at) VALUES (?, ?, ?)";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, expiresAt);
            int aff = ps.executeUpdate();
            if (aff == 0) return -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    // trova token per stringa
    public TokenRecord findByToken(String token) throws SQLException {
        String sql = "SELECT id, user_id, token, created_at, expires_at FROM verification_token WHERE token = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TokenRecord t = new TokenRecord();
                    t.id = rs.getInt("id");
                    t.userId = rs.getInt("user_id");
                    t.token = rs.getString("token");
                    t.createdAt = rs.getTimestamp("created_at");
                    t.expiresAt = rs.getTimestamp("expires_at");
                    return t;
                }
            }
        }
        return null;
    }

    // verifica e consuma token
    public boolean verifyAndConsumeToken(String token) throws SQLException {
        String updateSql = "UPDATE utente u JOIN verification_token vt ON vt.user_id = u.id "
                        + "SET u.email_verified = 1, u.email_verified_at = CURRENT_TIMESTAMP "
                        + "WHERE vt.token = ?";
        String deleteSql = "DELETE FROM verification_token WHERE token = ?";

        try (Connection c = ConnectionPool.getDataSource().getConnection()) {
            boolean prevAuto = c.getAutoCommit();
            try {
                c.setAutoCommit(false);
                // update utente
                try (PreparedStatement ps = c.prepareStatement(updateSql)) {
                    ps.setString(1, token);
                    int updated = ps.executeUpdate();
                    LOG.info("verifyAndConsumeToken: token=" + token + " updatedUsers=" + updated);
                    if (updated == 0) {
                        c.rollback();
                        LOG.info("verifyAndConsumeToken: nulla aggiornato -> rollback");
                        return false;
                    }
                }
                // delete token
                try (PreparedStatement ps2 = c.prepareStatement(deleteSql)) {
                    ps2.setString(1, token);
                    int del = ps2.executeUpdate();
                    LOG.info("verifyAndConsumeToken: token deleted rows=" + del);
                }
                c.commit();
                LOG.info("verifyAndConsumeToken: commit OK for token=" + token);
                return true;
                } catch (SQLException ex) {
                    try { c.rollback(); LOG.warning("verifyAndConsumeToken: rollback per token=" + token); } catch (SQLException ignore) {}
                    throw ex;
                } finally {
                    try { c.setAutoCommit(prevAuto); } catch (SQLException ignore) {}
                }
        }
    }



    // delete token by id
    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM verification_token WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            int aff = ps.executeUpdate();
            LOG.info("VerificationTokenDAO.deleteById: id=" + id + " affected=" + aff);
            return aff > 0;
        }
    }


    // small holder
    public static class TokenRecord {
        public int id;
        public int userId;
        public String token;
        public Timestamp createdAt;
        public Timestamp expiresAt;
    }
}
