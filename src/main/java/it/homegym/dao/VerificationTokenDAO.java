package it.homegym.dao;

import it.homegym.util.ConnectionPool;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.logging.Logger;

public class VerificationTokenDAO {
    private static final Logger LOG = Logger.getLogger(VerificationTokenDAO.class.getName());

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

    

    // delete token by id
    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM verification_token WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
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
