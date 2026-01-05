package it.homegym.dao;

import it.homegym.util.ConnectionPool;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

public class PasswordResetDAO {

    public static class TokenEntry {
        public int id;
        public int userId;
        public String token;
        public Timestamp expiresAt;
        public boolean used;
        public Timestamp createdAt;
    }

    public PasswordResetDAO() {
        if (ConnectionPool.getDataSource() == null) {
            throw new IllegalStateException("DataSource non inizializzato");
        }
    }

    public void createToken(int userId, String token, Timestamp expiresAt) throws SQLException {
        String sql = "INSERT INTO password_reset_token (user_id, token, expires_at) VALUES (?, ?, ?)";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();
        }
    }

    public Optional<TokenEntry> findValidByToken(String token) throws SQLException {
        String sql = "SELECT id, user_id, token, expires_at, used, created_at FROM password_reset_token WHERE token = ? AND used = 0 AND expires_at >= NOW()";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TokenEntry e = new TokenEntry();
                    e.id = rs.getInt("id");
                    e.userId = rs.getInt("user_id");
                    e.token = rs.getString("token");
                    e.expiresAt = rs.getTimestamp("expires_at");
                    e.used = rs.getInt("used") != 0;
                    e.createdAt = rs.getTimestamp("created_at");
                    return Optional.of(e);
                }
            }
        }
        return Optional.empty();
    }

    public boolean markUsed(String token) throws SQLException {
        String sql = "UPDATE password_reset_token SET used = 1 WHERE token = ? AND used = 0";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            return ps.executeUpdate() > 0;
        }
    }

    // opzionale: pulizia vecchi token
    public int deleteExpired() throws SQLException {
        String sql = "DELETE FROM password_reset_token WHERE expires_at < NOW()";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }
}
