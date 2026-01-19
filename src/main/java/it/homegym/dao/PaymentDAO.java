package it.homegym.dao;

import it.homegym.model.Payment;
import it.homegym.util.ConnectionPool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class PaymentDAO {

    public PaymentDAO() {
        if (ConnectionPool.getDataSource() == null) {
            throw new IllegalStateException("DataSource non inizializzato");
        }
    }

     public List<Payment> listAll() throws SQLException {
        return listPage(0, 1000); // fallback
    }

    public List<Payment> listPage(int offset, int limit) throws SQLException {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT id, user_id, amount, currency, status, created_at, updated_at FROM payment ORDER BY id DESC LIMIT ? OFFSET ?";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment p = new Payment();
                    p.setId(rs.getInt("id"));

                    Object userObj = rs.getObject("user_id");
                    if (userObj != null) {
                        if (userObj instanceof Number) {
                            p.setUserId(((Number) userObj).intValue());
                        } else {
                            p.setUserId(Integer.parseInt(userObj.toString()));
                        }
                    } else {
                        p.setUserId(null);
                    }

                    p.setAmount(rs.getBigDecimal("amount"));
                    p.setCurrency(rs.getString("currency"));
                    p.setStatus(rs.getString("status"));
                    p.setCreatedAt(rs.getTimestamp("created_at"));
                    p.setUpdatedAt(rs.getTimestamp("updated_at"));
                    list.add(p);
                }
            }
        }
        return list;
    }

    // Crea un nuovo pagamento e restituisce l'ID generato
    public int create(Payment p) throws SQLException {
        String sql = "INSERT INTO payment (user_id, amount, currency, status) VALUES (?, ?, ?, ?)";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (p.getUserId() != null) ps.setInt(1, p.getUserId()); else ps.setNull(1, Types.INTEGER);
            ps.setBigDecimal(2, p.getAmount());
            ps.setString(3, p.getCurrency());
            ps.setString(4, p.getStatus());

            int affected = ps.executeUpdate();
            if (affected == 0) return -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    p.setId(newId);
                    return newId;
                }
            }
        }
        return -1;
    }


     public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM payment";
        try (Connection con = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public boolean updateStatus(int id, String newStatus) throws SQLException {
        String sql = "UPDATE payment SET status = ? WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Payment findById(int id) throws SQLException {
        String sql = "SELECT id, user_id, amount, currency, status, created_at, updated_at FROM payment WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Payment p = new Payment();
                    p.setId(rs.getInt("id"));
                    int uid = rs.getInt("user_id");
                    p.setUserId(rs.wasNull() ? null : uid);
                    p.setAmount(rs.getBigDecimal("amount"));
                    p.setCurrency(rs.getString("currency"));
                    p.setStatus(rs.getString("status"));
                    p.setCreatedAt(rs.getTimestamp("created_at"));
                    p.setUpdatedAt(rs.getTimestamp("updated_at"));
                    return p;
                }
            }
        }
        return null;
    }

    public Stats stats() throws SQLException {
        String sql = "SELECT COUNT(*) AS total_payments, COALESCE(SUM(amount),0) AS total_amount FROM payment";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Stats s = new Stats();
            if (rs.next()) {
                s.totalPayments = rs.getInt("total_payments");
                s.totalAmount = rs.getBigDecimal("total_amount");
            }
            return s;
        }
    }

    public static class Stats {
        public int totalPayments;
        public BigDecimal totalAmount;
    }
}
