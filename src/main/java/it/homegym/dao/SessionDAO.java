package it.homegym.dao;

import it.homegym.model.TrainingSession;
import it.homegym.util.ConnectionPool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionDAO {

    public SessionDAO() {
        if (ConnectionPool.getDataSource() == null) {
            throw new IllegalStateException("DataSource non inizializzato");
        }
    }

    /**
     * Lista tutte le sessioni, con LEFT JOIN su utente per userName (se presente).
     */
    public List<TrainingSession> listAll() throws SQLException {
        String sql = "SELECT s.id, s.user_id, u.nome AS u_nome, u.cognome AS u_cognome, " +
                     "s.trainer, s.scheduled_at, s.duration_minutes, s.notes " +
                     "FROM session s LEFT JOIN utente u ON s.user_id = u.id " +
                     "ORDER BY s.scheduled_at DESC";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<TrainingSession> list = new ArrayList<>();
            while (rs.next()) {
                TrainingSession s = mapRow(rs);
                String nome = rs.getString("u_nome");
                String cognome = rs.getString("u_cognome");
                if (nome != null || cognome != null) {
                    String full = ((nome != null) ? nome : "") + ((cognome != null && !cognome.isBlank()) ? " " + cognome : "");
                    s.setUserName(full.trim());
                }
                list.add(s);
            }
            return list;
        }
    }

    public List<TrainingSession> listByTrainer(String trainer) throws SQLException {
        String sql = "SELECT s.id, s.user_id, u.nome AS u_nome, u.cognome AS u_cognome, " +
                     "s.trainer, s.scheduled_at, s.duration_minutes, s.notes " +
                     "FROM session s LEFT JOIN utente u ON s.user_id = u.id " +
                     "WHERE s.trainer = ? ORDER BY s.scheduled_at DESC";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, trainer);
            try (ResultSet rs = ps.executeQuery()) {
                List<TrainingSession> list = new ArrayList<>();
                while (rs.next()) {
                    TrainingSession s = mapRow(rs);
                    String nome = rs.getString("u_nome");
                    String cognome = rs.getString("u_cognome");
                    if (nome != null || cognome != null) {
                        String full = ((nome != null) ? nome : "") + ((cognome != null && !cognome.isBlank()) ? " " + cognome : "");
                        s.setUserName(full.trim());
                    }
                    list.add(s);
                }
                return list;
            }
        }
    }

    public TrainingSession findById(int id) throws SQLException {
        String sql = "SELECT s.id, s.user_id, u.nome AS u_nome, u.cognome AS u_cognome, " +
                     "s.trainer, s.scheduled_at, s.duration_minutes, s.notes " +
                     "FROM session s LEFT JOIN utente u ON s.user_id = u.id WHERE s.id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TrainingSession s = mapRow(rs);
                    String nome = rs.getString("u_nome");
                    String cognome = rs.getString("u_cognome");
                    if (nome != null || cognome != null) {
                        String full = ((nome != null) ? nome : "") + ((cognome != null && !cognome.isBlank()) ? " " + cognome : "");
                        s.setUserName(full.trim());
                    }
                    return s;
                }
            }
        }
        return null;
    }

    public boolean create(TrainingSession s) throws SQLException {
        String sql = "INSERT INTO session (user_id, trainer, scheduled_at, duration_minutes, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (s.getUserId() != null) ps.setInt(1, s.getUserId()); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, s.getTrainer());
            ps.setTimestamp(3, s.getWhen());
            ps.setInt(4, s.getDurationMinutes());
            ps.setString(5, s.getNotes());
            int affected = ps.executeUpdate();
            if (affected == 0) return false;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setId(keys.getInt(1));
            }
            return true;
        }
    }

    public boolean update(TrainingSession s) throws SQLException {
        String sql = "UPDATE session SET user_id = ?, trainer = ?, scheduled_at = ?, duration_minutes = ?, notes = ? WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (s.getUserId() != null) ps.setInt(1, s.getUserId()); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, s.getTrainer());
            ps.setTimestamp(3, s.getWhen());
            ps.setInt(4, s.getDurationMinutes());
            ps.setString(5, s.getNotes());
            ps.setInt(6, s.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM session WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }


    private TrainingSession mapRow(ResultSet rs) throws SQLException {
        TrainingSession s = new TrainingSession();
        s.setId(rs.getInt("id"));
        int uid = rs.getInt("user_id");
        s.setUserId(rs.wasNull() ? null : uid);
        s.setTrainer(rs.getString("trainer"));
        s.setWhen(rs.getTimestamp("scheduled_at"));
        s.setDurationMinutes(rs.getInt("duration_minutes"));
        s.setNotes(rs.getString("notes"));
        return s;
    }
    
}
