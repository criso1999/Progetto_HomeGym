package it.homegym.dao;

import it.homegym.model.Utente;
import it.homegym.util.ConnectionPool;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class UtenteDAO {

    public UtenteDAO() {
        if (ConnectionPool.getDataSource() == null) {
            throw new IllegalStateException("DataSource non inizializzato");
        }
    }

    public List<Utente> listAll() throws SQLException {
        List<Utente> list = new ArrayList<>();
        String sql = "SELECT id, nome, cognome, email, password, ruolo, created_at, trainer_id FROM utente ORDER BY id DESC";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Utente u = mapRow(rs);
                list.add(u);
            }
        }
        return list;
    }

    public List<Utente> listByRole(String ruolo) throws SQLException {
        List<Utente> list = new ArrayList<>();
        String sql = "SELECT id, nome, cognome, email, password, ruolo, created_at, trainer_id FROM utente WHERE ruolo = ? ORDER BY id DESC";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ruolo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Utente u = mapRow(rs);
                    list.add(u);
                }
            }
        }
        return list;
    }

    public List<Integer> listClientIdsByTrainer(int trainerId) throws Exception {
        String sql = "SELECT id FROM utente WHERE trainer_id = ?";
        List<Integer> out = new ArrayList<>();
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getInt("id"));
                }
            }
        }
        return out;
    }

    public boolean update(Utente u) throws SQLException {
        String sql = "UPDATE utente SET nome = ?, cognome = ?, email = ?, ruolo = ?, trainer_id = ? WHERE id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNome());
            ps.setString(2, u.getCognome());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getRuolo());
            if (u.getTrainerId() != null) ps.setInt(5, u.getTrainerId()); else ps.setNull(5, Types.INTEGER);
            ps.setInt(6, u.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateRole(int id, String newRole) throws SQLException {
        String sql = "UPDATE utente SET ruolo = ? WHERE id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newRole);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM utente WHERE id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Utente findById(int id) throws SQLException {
        String sql = "SELECT id, nome, cognome, email, password, ruolo, trainer_id FROM utente WHERE id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public boolean updatePassword(int id, String hashedPassword) throws SQLException {
        String sql = "UPDATE utente SET password = ? WHERE id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Utente findByEmail(String email) throws SQLException {
        String sql = "SELECT id, nome, cognome, email, password, ruolo, trainer_id, created_at FROM utente WHERE email = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public boolean create(Utente u) throws SQLException {
        String sql = "INSERT INTO utente (nome, cognome, email, password, ruolo, trainer_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, u.getNome());
            ps.setString(2, u.getCognome());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPassword()); // deve essere già hashed
            ps.setString(5, u.getRuolo());
            if (u.getTrainerId() != null) ps.setInt(6, u.getTrainerId()); else ps.setNull(6, Types.INTEGER);

            int affected = ps.executeUpdate();
            if (affected == 0) return false;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    u.setId(keys.getInt(1));
                }
            }
            return true;
        }
    }

     // Soft delete: marca deleted = 1 (non visibile nelle liste)
    public boolean softDeleteById(int id) throws SQLException {
        String sql = "UPDATE utente SET deleted = 1 WHERE id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Utente mapRow(ResultSet rs) throws SQLException {
        Utente u = new Utente();
        u.setId(rs.getInt("id"));
        u.setNome(rs.getString("nome"));
        u.setCognome(rs.getString("cognome"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setRuolo(rs.getString("ruolo"));
        try {
            int tid = rs.getInt("trainer_id");
            u.setTrainerId(rs.wasNull() ? null : tid);
        } catch (SQLException e) {
            // colonna trainer_id potrebbe non esistere in vecchi schema: ignorala se assente
            u.setTrainerId(null);
        }
        return u;
    }
}
