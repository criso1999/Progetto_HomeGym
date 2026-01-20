package it.homegym.dao;

import it.homegym.model.Presence;
import it.homegym.util.ConnectionPool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Date;

public class PresenceDAO {

    public PresenceDAO() {
        if (ConnectionPool.getDataSource() == null) throw new IllegalStateException("DataSource non inizializzato");
    }

    // crea token (UUID) e ritorna il token plaintext
    public String createToken(Integer sessionId, int expiresMinutes) throws SQLException {
        String token = UUID.randomUUID().toString();
        String sql = "INSERT INTO presenze (session_id, token, expires_at) VALUES (?, ?, ?)";
        Timestamp expires = new Timestamp(System.currentTimeMillis() + expiresMinutes * 60L * 1000L);
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (sessionId != null) ps.setInt(1, sessionId); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, token);
            ps.setTimestamp(3, expires);
            ps.executeUpdate();
        }
        return token;
    }

    // valida token non usato ed entro expiry; se valido consuma (set used, checkin_at, user_id, scanned_by)
    public boolean validateAndConsumeToken(String token, Integer userId, Integer scannerId) throws SQLException {
        String select = "SELECT id, used, expires_at FROM presenze WHERE token = ? FOR UPDATE";
        String update = "UPDATE presenze SET used = 1, user_id = ?, scanned_by = ?, checkin_at = ? WHERE id = ?";

        try (Connection c = ConnectionPool.getDataSource().getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(select)) {
                ps.setString(1, token);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { c.rollback(); return false; }
                    boolean used = rs.getBoolean("used");
                    Timestamp expires = rs.getTimestamp("expires_at");
                    int id = rs.getInt("id");

                    if (used) { c.rollback(); return false; }
                    if (expires != null && expires.before(new Timestamp(System.currentTimeMillis()))) { c.rollback(); return false; }

                    try (PreparedStatement ups = c.prepareStatement(update)) {
                        if (userId != null) ups.setInt(1, userId); else ups.setNull(1, Types.INTEGER);
                        if (scannerId != null) ups.setInt(2, scannerId); else ups.setNull(2, Types.INTEGER);
                        ups.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                        ups.setInt(4, id);
                        ups.executeUpdate();
                    }
                    c.commit();
                    return true;
                }
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public Presence findByToken(String token) throws SQLException {
        String sql = "SELECT id, session_id, user_id, token, used, scanned_by, checkin_at, expires_at, created_at FROM presenze WHERE token = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Presence p = new Presence();
                    p.setId(rs.getInt("id"));
                    int sid = rs.getInt("session_id"); if (!rs.wasNull()) p.setSessionId(sid);
                    int uid = rs.getInt("user_id"); if (!rs.wasNull()) p.setUserId(uid);
                    p.setToken(rs.getString("token"));
                    p.setUsed(rs.getBoolean("used"));
                    int sb = rs.getInt("scanned_by"); if (!rs.wasNull()) p.setScannedBy(sb);
                    p.setCheckinAt(rs.getTimestamp("checkin_at"));
                    p.setExpiresAt(rs.getTimestamp("expires_at"));
                    p.setCreatedAt(rs.getTimestamp("created_at"));
                    return p;
                }
            }
        }
        return null;
    }

    // lista presenze per sessione
    public List<Presence> listBySession(int sessionId) throws SQLException {
        List<Presence> out = new ArrayList<>();
        String sql = "SELECT id, session_id, user_id, token, used, scanned_by, checkin_at, expires_at, created_at FROM presenze WHERE session_id = ? ORDER BY created_at DESC";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Presence p = new Presence();
                    p.setId(rs.getInt("id"));
                    p.setSessionId(rs.getInt("session_id"));
                    int uid = rs.getInt("user_id"); if (!rs.wasNull()) p.setUserId(uid);
                    p.setToken(rs.getString("token"));
                    p.setUsed(rs.getBoolean("used"));
                    int sb = rs.getInt("scanned_by"); if (!rs.wasNull()) p.setScannedBy(sb);
                    p.setCheckinAt(rs.getTimestamp("checkin_at"));
                    p.setExpiresAt(rs.getTimestamp("expires_at"));
                    p.setCreatedAt(rs.getTimestamp("created_at"));
                    out.add(p);
                }
            }
        }
        return out;
    }
}
