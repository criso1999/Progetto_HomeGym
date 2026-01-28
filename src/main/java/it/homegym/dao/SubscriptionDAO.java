package it.homegym.dao;

import it.homegym.model.Subscription;
import it.homegym.model.SubscriptionPlan;
import it.homegym.util.ConnectionPool;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SubscriptionDAO {
    private static final Logger LOG = Logger.getLogger(SubscriptionDAO.class.getName());

    public List<SubscriptionPlan> listActivePlans() throws SQLException {
        String sql = "SELECT id, code, name, description, duration_days, price_cents, currency, active FROM subscription_plan WHERE active = 1 ORDER BY price_cents";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<SubscriptionPlan> out = new ArrayList<>();
            while (rs.next()) {
                SubscriptionPlan p = new SubscriptionPlan();
                p.setId(rs.getInt("id"));
                p.setCode(rs.getString("code"));
                p.setName(rs.getString("name"));
                p.setDescription(rs.getString("description"));
                p.setDurationDays(rs.getInt("duration_days"));
                p.setPriceCents(rs.getLong("price_cents"));
                p.setCurrency(rs.getString("currency"));
                p.setActive(rs.getBoolean("active"));
                out.add(p);
            }
            return out;
        }
    }

    public SubscriptionPlan findPlanById(int planId) throws SQLException {
        String sql = "SELECT id, code, name, description, duration_days, price_cents, currency, active FROM subscription_plan WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SubscriptionPlan p = new SubscriptionPlan();
                    p.setId(rs.getInt("id"));
                    p.setCode(rs.getString("code"));
                    p.setName(rs.getString("name"));
                    p.setDescription(rs.getString("description"));
                    p.setDurationDays(rs.getInt("duration_days"));
                    p.setPriceCents(rs.getLong("price_cents"));
                    p.setCurrency(rs.getString("currency"));
                    p.setActive(rs.getBoolean("active"));
                    return p;
                }
            }
        }
        return null;
    }

    public int createSubscription(Subscription sub) throws SQLException {
        String sql = "INSERT INTO subscription (user_id, plan_id, status, price_cents, currency, start_date, end_date, payment_provider, payment_provider_subscription_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sub.getUserId());
            ps.setInt(2, sub.getPlanId());
            ps.setString(3, sub.getStatus());
            ps.setLong(4, sub.getPriceCents());
            ps.setString(5, sub.getCurrency());
            if (sub.getStartDate() != null) ps.setDate(6, Date.valueOf(sub.getStartDate())); else ps.setNull(6, Types.DATE);
            if (sub.getEndDate() != null) ps.setDate(7, Date.valueOf(sub.getEndDate())); else ps.setNull(7, Types.DATE);
            ps.setString(8, sub.getPaymentProvider());
            ps.setString(9, sub.getPaymentProviderSubscriptionId());
            int aff = ps.executeUpdate();
            if (aff == 0) return -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public Subscription getActiveSubscriptionForUser(int userId) throws SQLException {
        String sql = "SELECT id, user_id, plan_id, status, price_cents, currency, start_date, end_date, payment_provider, payment_provider_subscription_id FROM subscription WHERE user_id = ? AND status = 'ACTIVE' ORDER BY end_date DESC LIMIT 1";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Subscription s = mapSubscription(rs);
                    return s;
                }
            }
        }
        return null;
    }

    private Subscription mapSubscription(ResultSet rs) throws SQLException {
        Subscription s = new Subscription();
        s.setId(rs.getInt("id"));
        s.setUserId(rs.getInt("user_id"));
        s.setPlanId(rs.getInt("plan_id"));
        s.setStatus(rs.getString("status"));
        s.setPriceCents(rs.getLong("price_cents"));
        s.setCurrency(rs.getString("currency"));
        Date sd = rs.getDate("start_date"); if (sd != null) s.setStartDate(sd.toLocalDate());
        Date ed = rs.getDate("end_date"); if (ed != null) s.setEndDate(ed.toLocalDate());
        s.setPaymentProvider(rs.getString("payment_provider"));
        s.setPaymentProviderSubscriptionId(rs.getString("payment_provider_subscription_id"));
        return s;
    }

    public List<Subscription> listExpiringSubscriptions(LocalDate beforeOrOn) throws SQLException {
        String sql = "SELECT id, user_id, plan_id, status, price_cents, currency, start_date, end_date, payment_provider, payment_provider_subscription_id FROM subscription WHERE status = 'ACTIVE' AND end_date <= ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(beforeOrOn));
            try (ResultSet rs = ps.executeQuery()) {
                List<Subscription> out = new ArrayList<>();
                while (rs.next()) out.add(mapSubscription(rs));
                return out;
            }
        }
    }

    public boolean markSubscriptionExpired(int subscriptionId) throws SQLException {
        String sql = "UPDATE subscription SET status = 'EXPIRED' WHERE id = ? AND status = 'ACTIVE'";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, subscriptionId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean cancelSubscription(int subscriptionId) throws SQLException {
        String sql = "UPDATE subscription SET status = 'CANCELLED' WHERE id = ? AND status IN ('PENDING','ACTIVE')";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, subscriptionId);
            return ps.executeUpdate() > 0;
        }
    }

    // possibilità: renew/create active with startDate = yesterday+1 + endDate computed by plan duration
    public boolean activateSubscription(int subscriptionId, LocalDate startDate, LocalDate endDate, String providerId) throws SQLException {
        String sql = "UPDATE subscription SET status = 'ACTIVE', start_date = ?, end_date = ?, payment_provider_subscription_id = ? WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            ps.setString(3, providerId);
            ps.setInt(4, subscriptionId);
            return ps.executeUpdate() > 0;
        }
    }
}
