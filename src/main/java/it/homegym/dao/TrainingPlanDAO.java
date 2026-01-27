package it.homegym.dao;

import it.homegym.model.TrainingPlan;
import it.homegym.model.TrainingPlanVersion;
import it.homegym.model.TrainingPlanAssignment;
import it.homegym.model.TrainingPlanAttachment;
import it.homegym.util.ConnectionPool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO per training_plan, training_plan_version, training_plan_assignment.
 * Aggiornato addAttachment con detection robusta della tabella training_plan_attachment
 * e logging dettagliato per facilitare il debug quando la tabella non viene popolata.
 *
 * Correzioni principali:
 *  - listPlansByTrainer ora seleziona anche i campi attachment_* (prima venivano lasciati fuori,
 *    causando valori null in mapPlan e quindi la JSP che mostrava "Nessuno").
 *  - piccole migliorie al logging e gestione degli statement/rs.
 */
public class TrainingPlanDAO {

    private static final Logger LOG = Logger.getLogger(TrainingPlanDAO.class.getName());

    public TrainingPlanDAO() {
        if (ConnectionPool.getDataSource() == null) throw new IllegalStateException("DataSource non inizializzato");
    }

    // --- compatibilità / wrapper ------------------------------------------------

    /**
     * Wrapper compatibile con servlet esistente: crea piano e ritorna id.
     */
    public int create(TrainingPlan p) throws SQLException {
        return createPlan(p);
    }

    /**
     * Wrapper compatibile per soft-delete.
     */
    public boolean softDelete(int planId) throws SQLException {
        return softDeletePlan(planId);
    }

    // Add attachment convenience: minimal signature (filename + path). Overload disponibile.
    public boolean addAttachment(int planId, String filename, String path) throws SQLException {
        return addAttachment(planId, filename, path, null, null);
    }

    /**
     * Aggiunge un attachment. Prima prova ad inserire nella tabella training_plan_attachment (se presente).
     * Se la tabella non esiste o si verifica errore, fa fallback aggiornando i campi attachment_* della tabella training_plan.
     *
     * @param planId id piano
     * @param filename nome file originale
     * @param path percorso relativo/URL salvato
     * @param size dimensione in byte (nullable)
     * @param contentType content type (nullable)
     * @return true se registrato correttamente (in attachment table o su training_plan)
     */
    public boolean addAttachment(int planId, String filename, String path, Long size, String contentType) throws SQLException {
        try (Connection c = ConnectionPool.getDataSource().getConnection()) {
            // Proviamo a vedere se esiste la tabella training_plan_attachment.
            boolean attachmentTableExists = tableExists(c, "training_plan_attachment");
            if (attachmentTableExists) {
                // Proviamo a fare INSERT nella tabella attachment.
                String insertAttachSql = "INSERT INTO training_plan_attachment (plan_id, filename, path, content_type, size, uploaded_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
                try (PreparedStatement ps = c.prepareStatement(insertAttachSql)) {
                    ps.setInt(1, planId);
                    ps.setString(2, filename);
                    ps.setString(3, path);
                    if (contentType != null) ps.setString(4, contentType); else ps.setNull(4, Types.VARCHAR);
                    if (size != null) ps.setLong(5, size); else ps.setNull(5, Types.BIGINT);
                    int affected = ps.executeUpdate();
                    if (affected > 0) {
                        LOG.log(Level.INFO, "Inserito attachment su training_plan_attachment: planId={0}, file={1}", new Object[]{planId, filename});
                        return true;
                    } else {
                        LOG.log(Level.WARNING, "INSERT in training_plan_attachment non ha inserito righe: planId={0}, file={1}", new Object[]{planId, filename});
                    }
                } catch (SQLException insertEx) {
                    // Se fallisce l'insert, logghiamo e procediamo al fallback.
                    LOG.log(Level.WARNING, "Errore INSERT in training_plan_attachment: {0}", insertEx.getMessage());
                    LOG.log(Level.FINE, "Dettaglio SQLException:", insertEx);
                    // non rilanciamo subito: proviamo fallback su training_plan
                }
            } else {
                LOG.log(Level.INFO, "Tabella training_plan_attachment non trovata: fallback su training_plan (planId={0})", planId);
            }

            // Fallback: aggiorna training_plan attachment_*
            String updatePlanSql = "UPDATE training_plan SET attachment_filename = ?, attachment_content_type = ?, attachment_path = ?, attachment_size = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement ps2 = c.prepareStatement(updatePlanSql)) {
                ps2.setString(1, filename);
                if (contentType != null) ps2.setString(2, contentType); else ps2.setNull(2, Types.VARCHAR);
                ps2.setString(3, path);
                if (size != null) ps2.setLong(4, size); else ps2.setNull(4, Types.BIGINT);
                ps2.setInt(5, planId);
                int updated = ps2.executeUpdate();
                if (updated > 0) {
                    LOG.log(Level.INFO, "Aggiornati campi attachment in training_plan per planId={0}, file={1}", new Object[]{planId, filename});
                    return true;
                } else {
                    LOG.log(Level.WARNING, "UPDATE training_plan non ha aggiornato righe per planId={0}", planId);
                }
            } catch (SQLException updateEx) {
                LOG.log(Level.SEVERE, "Errore UPDATE training_plan per planId={0}: {1}", new Object[]{planId, updateEx.getMessage()});
                LOG.log(Level.FINE, "Dettaglio SQLException:", updateEx);
                throw updateEx; // non riuscito fallback => segnaliamo l'errore
            }
        }

        // se arriviamo qui, non è stato possibile registrare l'allegato
        return false;
    }

    /**
     * Verifica se una tabella esiste nel DB in modo robusto: prova il nome così com'è,
     * poi in uppercase e lowercase (alcuni DB memorizzano nomi in uppercase).
     */
    private boolean tableExists(Connection c, String tableName) {
        try {
            DatabaseMetaData md = c.getMetaData();
            // Proviamo esatto, uppercase e lowercase.
            String[] trials = new String[] { tableName, tableName.toUpperCase(), tableName.toLowerCase() };
            for (String t : trials) {
                try (ResultSet rs = md.getTables(null, null, t, new String[] {"TABLE"})) {
                    if (rs.next()) {
                        LOG.log(Level.FINE, "Trovata tabella con nome: {0}", t);
                        return true;
                    }
                }
            }
            // Alcuni DB richiedono il catalog/schema; proviamo pattern con %
            try (ResultSet rs2 = md.getTables(null, "%", "%" + tableName + "%", new String[] {"TABLE"})) {
                while (rs2.next()) {
                    String found = rs2.getString("TABLE_NAME");
                    if (found != null && found.toLowerCase().contains(tableName.toLowerCase())) {
                        LOG.log(Level.FINE, "Trovata tabella (pattern) TABLE_NAME={0}", found);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Impossibile leggere DatabaseMetaData: {0}", e.getMessage());
            LOG.log(Level.FINER, "Dettaglio SQLException:", e);
        }
        return false;
    }

    // --- esistente (modificato leggermente) ------------------------------------

    // Create plan (version_number = 1 inserted into version table)
    public int createPlan(TrainingPlan p) throws SQLException {
        String sql = "INSERT INTO training_plan (title, description, content, created_by) VALUES (?, ?, ?, ?)";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getContent());
            if (p.getCreatedBy() != null) ps.setInt(4, p.getCreatedBy()); else ps.setNull(4, Types.INTEGER);
            int affected = ps.executeUpdate();
            if (affected == 0) return -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    p.setId(id);
                    // insert initial version
                    insertVersion(c, id, 1, p);
                    return id;
                }
            }
        }
        return -1;
    }

    // Insert version helper (uses existing connection to keep consistent)
    private void insertVersion(Connection c, int planId, int versionNumber, TrainingPlan p) throws SQLException {
        String sql = "INSERT INTO training_plan_version (plan_id, version_number, title, description, content, created_by) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, planId);
            ps.setInt(2, versionNumber);
            ps.setString(3, p.getTitle());
            ps.setString(4, p.getDescription());
            ps.setString(5, p.getContent());
            if (p.getCreatedBy() != null) ps.setInt(6, p.getCreatedBy()); else ps.setNull(6, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    // inserisci e ritorna id dell'attachment (o -1 se fallisce)
    public int insertAttachment(int planId, String filename, String path, Long size, String contentType, Integer uploadedBy) throws SQLException {
        String sql = "INSERT INTO training_plan_attachment (plan_id, filename, path, content_type, size, uploaded_at, uploaded_by) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, planId);
            ps.setString(2, filename);
            ps.setString(3, path);
            if (contentType != null) ps.setString(4, contentType); else ps.setNull(4, Types.VARCHAR);
            if (size != null) ps.setLong(5, size); else ps.setNull(5, Types.BIGINT);
            if (uploadedBy != null) ps.setInt(6, uploadedBy); else ps.setNull(6, Types.INTEGER);

            int affected = ps.executeUpdate();
            if (affected == 0) return -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    // lista attachments per plan (più recente prima)
    public List<TrainingPlanAttachment> listAttachmentsForPlan(int planId) throws SQLException {
        List<TrainingPlanAttachment> out = new ArrayList<>();
        String sql = "SELECT id, plan_id, filename, path, content_type, size, uploaded_at, uploaded_by FROM training_plan_attachment WHERE plan_id = ? ORDER BY uploaded_at DESC";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TrainingPlanAttachment a = new TrainingPlanAttachment();
                    a.setId(rs.getInt("id"));
                    a.setPlanId(rs.getInt("plan_id"));
                    a.setFilename(rs.getString("filename"));
                    a.setPath(rs.getString("path"));
                    a.setContentType(rs.getString("content_type"));
                    long s = rs.getLong("size"); a.setSize(rs.wasNull() ? null : s);
                    Timestamp t = rs.getTimestamp("uploaded_at"); if (t != null) a.setUploadedAt(new java.util.Date(t.getTime()));
                    int ub = rs.getInt("uploaded_by"); a.setUploadedBy(rs.wasNull() ? null : ub);
                    out.add(a);
                }
            }
        }
        return out;
    }

    // ritorna l'ultimo attachment (o null)
    public TrainingPlanAttachment findLatestAttachmentForPlan(int planId) throws SQLException {
        String sql = "SELECT id, plan_id, filename, path, content_type, size, uploaded_at, uploaded_by FROM training_plan_attachment WHERE plan_id = ? ORDER BY uploaded_at DESC LIMIT 1";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TrainingPlanAttachment a = new TrainingPlanAttachment();
                    a.setId(rs.getInt("id"));
                    a.setPlanId(rs.getInt("plan_id"));
                    a.setFilename(rs.getString("filename"));
                    a.setPath(rs.getString("path"));
                    a.setContentType(rs.getString("content_type"));
                    long s = rs.getLong("size"); a.setSize(rs.wasNull() ? null : s);
                    Timestamp t = rs.getTimestamp("uploaded_at"); if (t != null) a.setUploadedAt(new java.util.Date(t.getTime()));
                    int ub = rs.getInt("uploaded_by"); a.setUploadedBy(rs.wasNull() ? null : ub);
                    return a;
                }
            }
        }
        return null;
    }

    // opzionale: elimina attachment
    public boolean deleteAttachmentById(int attachmentId) throws SQLException {
        String sql = "DELETE FROM training_plan_attachment WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, attachmentId);
            return ps.executeUpdate() > 0;
        }
    }


    // Update plan -> save previous state into version table (increment version)
    public boolean updatePlan(TrainingPlan p, int editorId) throws SQLException {
        try (Connection c = ConnectionPool.getDataSource().getConnection()) {
            c.setAutoCommit(false);
            try {
                // read current to compute version_number
                int nextVersion = 1;
                String vsql = "SELECT MAX(version_number) AS mv FROM training_plan_version WHERE plan_id = ?";
                try (PreparedStatement vps = c.prepareStatement(vsql)) {
                    vps.setInt(1, p.getId());
                    try (ResultSet rs = vps.executeQuery()) {
                        if (rs.next()) {
                            int mv = rs.getInt("mv");
                            if (!rs.wasNull()) nextVersion = mv + 1;
                        }
                    }
                }

                // update main row
                String up = "UPDATE training_plan SET title = ?, description = ?, content = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                try (PreparedStatement ups = c.prepareStatement(up)) {
                    ups.setString(1, p.getTitle());
                    ups.setString(2, p.getDescription());
                    ups.setString(3, p.getContent());
                    ups.setInt(4, p.getId());
                    ups.executeUpdate();
                }

                // insert version snapshot
                TrainingPlan snapshot = new TrainingPlan();
                snapshot.setTitle(p.getTitle());
                snapshot.setDescription(p.getDescription());
                snapshot.setContent(p.getContent());
                snapshot.setCreatedBy(editorId);
                insertVersion(c, p.getId(), nextVersion, snapshot);

                c.commit();
                return true;
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public boolean update(TrainingPlan p) throws SQLException {
        String sql = "UPDATE training_plan SET title = ?, description = ?, content = ?, attachment_filename = ?, attachment_content_type = ?, attachment_path = ?, attachment_size = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getContent());
            ps.setString(4, p.getAttachmentFilename());
            ps.setString(5, p.getAttachmentContentType());
            ps.setString(6, p.getAttachmentPath());
            if (p.getAttachmentSize() != null) ps.setLong(7, p.getAttachmentSize()); else ps.setNull(7, Types.BIGINT);
            ps.setInt(8, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // Assign plan to user
    public int assignPlanToUser(int planId, int userId, int trainerId, String notes) throws SQLException {
        String sql = "INSERT INTO training_plan_assignment (plan_id, user_id, trainer_id, notes) VALUES (?, ?, ?, ?)";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, planId);
            ps.setInt(2, userId);
            ps.setInt(3, trainerId);
            ps.setString(4, notes);
            int affected = ps.executeUpdate();
            if (affected == 0) return -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    // List plans created by a trainer
    public List<TrainingPlan> listPlansByTrainer(int trainerId) throws SQLException {
        List<TrainingPlan> out = new ArrayList<>();
        // ADJUSTED: include attachment columns so mapPlan can populate them and JSP will show them
        String sql = "SELECT id, title, description, content, created_by, created_at, updated_at, deleted, " +
                     "attachment_filename, attachment_content_type, attachment_path, attachment_size " +
                     "FROM training_plan WHERE created_by = ? AND (deleted = 0 OR deleted IS NULL) ORDER BY updated_at DESC";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TrainingPlan p = mapPlan(rs);
                    out.add(p);
                }
            }
        }
        return out;
    }

    // Get plan by id
    public TrainingPlan findById(int id) throws SQLException {
        String sql = "SELECT id, title, description, content, created_by, created_at, updated_at, deleted, "
                + "attachment_filename, attachment_content_type, attachment_path, attachment_size "
                + "FROM training_plan WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TrainingPlan p = mapPlan(rs);

                    // --- PROVA A LEGGERE L'ULTIMO ATTACHMENT DALLA TABELLA training_plan_attachment (se presente)
                    String attachSql = "SELECT filename, path, content_type, size FROM training_plan_attachment WHERE plan_id = ? ORDER BY uploaded_at DESC LIMIT 1";
                    try (PreparedStatement aps = c.prepareStatement(attachSql)) {
                        aps.setInt(1, id);
                        try (ResultSet ars = aps.executeQuery()) {
                            if (ars.next()) {
                                String aFilename = ars.getString("filename");
                                String aPath = ars.getString("path");
                                String aContentType = ars.getString("content_type");
                                long aSize = ars.getLong("size");
                                p.setAttachmentFilename(aFilename);
                                p.setAttachmentPath(aPath);
                                p.setAttachmentContentType(aContentType);
                                p.setAttachmentSize(ars.wasNull() ? null : aSize);
                            }
                        }
                    } catch (SQLException ignore) {
                        LOG.log(Level.FINE, "Unable to read training_plan_attachment for plan {0}: {1}", new Object[]{id, ignore.getMessage()});
                    }

                    return p;
                }
            }
        }
        return null;
    }

    private TrainingPlan mapPlan(ResultSet rs) throws SQLException {
        TrainingPlan p = new TrainingPlan();
        p.setId(rs.getInt("id"));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setContent(rs.getString("content"));

        // created_by nullable
        try {
            int cb = rs.getInt("created_by");
            p.setCreatedBy(rs.wasNull() ? null : cb);
        } catch (SQLException ignored) {
            p.setCreatedBy(null);
        }

        Timestamp ca = null;
        try { ca = rs.getTimestamp("created_at"); } catch (SQLException ignored) {}
        if (ca != null) p.setCreatedAt(new java.util.Date(ca.getTime()));

        Timestamp ua = null;
        try { ua = rs.getTimestamp("updated_at"); } catch (SQLException ignored) {}
        if (ua != null) p.setUpdatedAt(new java.util.Date(ua.getTime()));

        // attachments (nullable)
        try {
            p.setAttachmentFilename(rs.getString("attachment_filename"));
        } catch (SQLException ignored) {}
        try {
            p.setAttachmentContentType(rs.getString("attachment_content_type"));
        } catch (SQLException ignored) {}
        try {
            p.setAttachmentPath(rs.getString("attachment_path"));
        } catch (SQLException ignored) {}
        try {
            long as = rs.getLong("attachment_size");
            if (rs.wasNull()) p.setAttachmentSize(null); else p.setAttachmentSize(as);
        } catch (SQLException ignored) {}

        p.setDeleted(rs.getBoolean("deleted"));
        return p;
    }

    // List assignments for user
    public List<TrainingPlanAssignment> listAssignmentsForUser(int userId) throws SQLException {
        List<TrainingPlanAssignment> out = new ArrayList<>();
        String sql = "SELECT id, plan_id, user_id, trainer_id, assigned_at, active, notes, payment_id FROM training_plan_assignment WHERE user_id = ? ORDER BY assigned_at DESC";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TrainingPlanAssignment a = new TrainingPlanAssignment();
                    a.setId(rs.getInt("id"));
                    a.setPlanId(rs.getInt("plan_id"));
                    a.setUserId(rs.getInt("user_id"));
                    a.setTrainerId(rs.getInt("trainer_id"));
                    a.setAssignedAt(rs.getTimestamp("assigned_at"));
                    a.setActive(rs.getBoolean("active"));
                    a.setNotes(rs.getString("notes"));
                    int pid = rs.getInt("payment_id");
                    a.setPaymentId(rs.wasNull() ? null : pid);
                    out.add(a);
                }
            }
        }
        return out;
    }

    // Get plan history (versions)
    public List<TrainingPlanVersion> getPlanHistory(int planId) throws SQLException {
        List<TrainingPlanVersion> out = new ArrayList<>();
        String sql = "SELECT id, plan_id, version_number, title, description, content, created_by, created_at FROM training_plan_version WHERE plan_id = ? ORDER BY version_number DESC";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TrainingPlanVersion v = new TrainingPlanVersion();
                    v.setId(rs.getInt("id"));
                    v.setPlanId(rs.getInt("plan_id"));
                    v.setVersionNumber(rs.getInt("version_number"));
                    v.setTitle(rs.getString("title"));
                    v.setDescription(rs.getString("description"));
                    v.setContent(rs.getString("content"));
                    v.setCreatedBy(rs.getInt("created_by"));
                    v.setCreatedAt(rs.getTimestamp("created_at"));
                    out.add(v);
                }
            }
        }
        return out;
    }

    // Soft-delete plan
    public boolean softDeletePlan(int planId) throws SQLException {
        String sql = "UPDATE training_plan SET deleted = 1 WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, planId);
            return ps.executeUpdate() > 0;
        }
    }

    // List all plans (admin)
    public List<TrainingPlan> listAllPlans() throws SQLException {
        List<TrainingPlan> out = new ArrayList<>();
        String sql = "SELECT id, title, description, content, created_by, created_at, updated_at, deleted, attachment_filename, attachment_content_type, attachment_path, attachment_size FROM training_plan ORDER BY updated_at DESC";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(mapPlan(rs));
            }
        }
        return out;
    }

    // Reactivate assignment
    public boolean setAssignmentActive(int assignmentId, boolean active) throws SQLException {
        String sql = "UPDATE training_plan_assignment SET active = ? WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, assignmentId);
            return ps.executeUpdate() > 0;
        }
    }
}
