package it.homegym.dao;

import it.homegym.model.TrainingPlan;
import it.homegym.model.TrainingPlanVersion;
import it.homegym.model.TrainingPlanAssignment;
import it.homegym.util.ConnectionPool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrainingPlanDAO {

    public TrainingPlanDAO() {
        if (ConnectionPool.getDataSource() == null) throw new IllegalStateException("DataSource non inizializzato");
    }

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
                        if (rs.next()) nextVersion = rs.getInt("mv") + 1;
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
        String sql = "SELECT id, title, description, content, created_by, created_at, updated_at, deleted FROM training_plan WHERE created_by = ? AND (deleted = 0 OR deleted IS NULL) ORDER BY updated_at DESC";
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
        String sql = "SELECT id, title, description, content, created_by, created_at, updated_at, deleted FROM training_plan WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPlan(rs);
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
        try { p.setCreatedBy(rs.getInt("created_by")); } catch (SQLException ignored) {}
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(new java.util.Date(ca.getTime()));
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) p.setUpdatedAt(new java.util.Date(ua.getTime()));
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
