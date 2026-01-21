package it.homegym.dao;

import it.homegym.model.Utente;
import it.homegym.util.ConnectionPool;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UtenteDAO {

    public UtenteDAO() {
        if (ConnectionPool.getDataSource() == null) {
            throw new IllegalStateException("DataSource non inizializzato");
        }
    }

    /**
     * Lista tutti gli utenti (admin). NON filtra deleted.
     */
    public List<Utente> listAll() throws SQLException {
        List<Utente> list = new ArrayList<>();
        String sql = "SELECT id, nome, cognome, email, password, ruolo, created_at, trainer_id, deleted " +
                     "FROM utente ORDER BY id DESC";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Lista utenti per ruolo escludendo quelli soft-deleted.
     */
    public List<Utente> listByRole(String ruolo) throws SQLException {
        List<Utente> list = new ArrayList<>();
        String sql = "SELECT id, nome, cognome, email, password, ruolo, created_at, trainer_id, deleted " +
                     "FROM utente " +
                     "WHERE ruolo = ? AND (deleted = 0 OR deleted IS NULL) " +
                     "ORDER BY id DESC";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ruolo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // Ritorna i clienti assegnati al trainer, INCLUDENDO anche quelli soft-deleted
    public List<Utente> listClientsByTrainerIncludingDeleted(int trainerId) throws SQLException {
        List<Utente> list = new ArrayList<>();
        String sql = "SELECT id, nome, cognome, email, password, ruolo, created_at, trainer_id, deleted " +
                    "FROM utente " +
                    "WHERE trainer_id = ? " +
                    "ORDER BY id DESC";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Ritorna solo gli ID dei clienti assegnati al trainer (usato da vari controller).
     */
    public List<Integer> listClientIdsByTrainer(int trainerId) throws SQLException {
        List<Integer> out = new ArrayList<>();
        String sql = "SELECT id FROM utente WHERE trainer_id = ? AND (deleted = 0 OR deleted IS NULL)";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getInt("id"));
            }
        }
        return out;
    }

    /**
     * Alias compatibilità: alcuni servlet chiamano listClient(...)
     * -> ritorna list di id per compatibilità col codice esistente.
     */
    public List<Integer> listClient(int trainerId) throws SQLException {
        return listClientIdsByTrainer(trainerId);
    }

    /**
     * Lista clienti disponibili per assegnazione (ruolo CLIENTE, senza trainer, non deleted)
     */
    public List<Utente> listAvailableClientsForAssign() throws SQLException {
        List<Utente> list = new ArrayList<>();
        String sql = "SELECT id, nome, cognome, email, password, ruolo, created_at, trainer_id, deleted " +
                     "FROM utente " +
                     "WHERE (trainer_id IS NULL) AND ruolo = 'CLIENTE' AND (deleted = 0 OR deleted IS NULL) " +
                     "ORDER BY id DESC";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
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
        String sql = "SELECT id, nome, cognome, email, password, ruolo, trainer_id, deleted, created_at FROM utente WHERE id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
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
        String sql = "SELECT id, nome, cognome, email, password, ruolo, trainer_id, created_at, deleted FROM utente WHERE email = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
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
                if (keys.next()) u.setId(keys.getInt(1));
            }
            return true;
        }
    }

    // Assegna trainer ad un utente esistente (set trainer_id, imposta ruolo CLIENTE)
    public boolean assignTrainerToUser(int userId, int trainerId) throws SQLException {
        String sql = "UPDATE utente SET trainer_id = ?, ruolo = 'CLIENTE' WHERE id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, trainerId, java.sql.Types.INTEGER);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // Soft delete: imposta deleted = 1 (true) invece di cancellare
    public boolean softDeleteById(int id) throws SQLException {
        String sql = "UPDATE utente SET deleted = 1 WHERE id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Ripristina un utente soft-deleted (deleted = 0)
    public boolean restoreById(int id) throws SQLException {
        String sql = "UPDATE utente SET deleted = 0 WHERE id = ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

        /**
     * Ritorna i clienti (Utente) assegnati al trainer, escludendo quelli soft-deleted.
     */
    public List<Utente> listClientsByTrainer(int trainerId) throws SQLException {
        List<Utente> list = new ArrayList<>();
        String sql = "SELECT id, nome, cognome, email, password, ruolo, created_at, trainer_id, deleted " +
                     "FROM utente " +
                     "WHERE trainer_id = ? AND ruolo = 'CLIENTE' AND (deleted = 0 OR deleted IS NULL) " +
                     "ORDER BY id DESC";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }


    // ---- helper ----
    private Utente mapRow(ResultSet rs) throws SQLException {
        Utente u = new Utente();
        u.setId(rs.getInt("id"));
        u.setNome(rs.getString("nome"));
        u.setCognome(rs.getString("cognome"));
        u.setEmail(rs.getString("email"));
        try { u.setPassword(rs.getString("password")); } catch (SQLException ignored) {}
        try { u.setRuolo(rs.getString("ruolo")); } catch (SQLException ignored) {}

        // trainer_id (nullable)
        try {
            int tid = rs.getInt("trainer_id");
            u.setTrainerId(rs.wasNull() ? null : tid);
        } catch (SQLException ignored) {
            u.setTrainerId(null);
        }

        // deleted (nullable)
        try {
            boolean d = rs.getBoolean("deleted");
            if (rs.wasNull()) u.setDeleted(false); else u.setDeleted(d);
        } catch (SQLException ignored) {
            u.setDeleted(false);
        }

        // created_at (nullable)
        try {
            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) u.setCreatedAt(new Date(ts.getTime()));
        } catch (SQLException ignored) {}

        return u;
    }
}
