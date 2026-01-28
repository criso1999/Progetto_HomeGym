package it.homegym.dao;

import it.homegym.model.Subscription;
import it.homegym.model.SubscriptionPlan;
import it.homegym.util.ConnectionPool;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO per gestione piani di abbonamento e sottoscrizioni.
 * Metodi principali:
 *  - listActivePlans(), listAllPlans(), findPlanById(), createPlan(), updatePlan(), deletePlan()
 *  - createSubscription(), listAllSubscriptions(), getActiveSubscriptionForUser()
 *  - activateSubscription(subscriptionId) overload che calcola start/end in base al piano
 *  - cancelSubscription(), markSubscriptionExpired()
 */
public class SubscriptionDAO {
    private static final Logger LOG = Logger.getLogger(SubscriptionDAO.class.getName());

    // ------------------- Piani -------------------

    /**
     * Lista solo piani attivi (per la view pubblica)
     */
    public List<SubscriptionPlan> listActivePlans() throws SQLException {
        String sql = "SELECT id, code, name, description, duration_days, price_cents, currency, active FROM subscription_plan WHERE active = 1 ORDER BY price_cents";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<SubscriptionPlan> out = new ArrayList<>();
            while (rs.next()) out.add(mapPlan(rs));
            return out;
        }
    }

    /**
     * Lista tutti i piani (admin)
     */
    public List<SubscriptionPlan> listAllPlans() throws SQLException {
        String sql = "SELECT id, code, name, description, duration_days, price_cents, currency, active FROM subscription_plan ORDER BY price_cents";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<SubscriptionPlan> out = new ArrayList<>();
            while (rs.next()) out.add(mapPlan(rs));
            return out;
        }
    }

    private SubscriptionPlan mapPlan(ResultSet rs) throws SQLException {
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

    /**
     * Ritorna un piano per id (anche se non attivo).
     */
    public SubscriptionPlan findPlanById(int planId) throws SQLException {
        String sql = "SELECT id, code, name, description, duration_days, price_cents, currency, active FROM subscription_plan WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPlan(rs);
            }
        }
        return null;
    }

    /**
     * Crea un piano e ritorna l'id generato (o -1 in caso di errore).
     */
    public int createPlan(SubscriptionPlan plan) throws SQLException {
        String sql = "INSERT INTO subscription_plan (code, name, description, duration_days, price_cents, currency, active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plan.getCode());
            ps.setString(2, plan.getName());
            ps.setString(3, plan.getDescription());
            if (plan.getDurationDays() != null) ps.setInt(4, plan.getDurationDays()); else ps.setNull(4, Types.INTEGER);
            ps.setLong(5, plan.getPriceCents());
            ps.setString(6, plan.getCurrency());
            ps.setBoolean(7, plan.getActive());
            int aff = ps.executeUpdate();
            if (aff == 0) return -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    /**
     * Aggiorna un piano. Ritorna true se aggiornato.
     */
    public boolean updatePlan(SubscriptionPlan plan) throws SQLException {
        String sql = "UPDATE subscription_plan SET code = ?, name = ?, description = ?, duration_days = ?, price_cents = ?, currency = ?, active = ? WHERE id = ?";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, plan.getCode());
            ps.setString(2, plan.getName());
            ps.setString(3, plan.getDescription());
            if (plan.getDurationDays() != null) ps.setInt(4, plan.getDurationDays()); else ps.setNull(4, Types.INTEGER);
            ps.setLong(5, plan.getPriceCents());
            ps.setString(6, plan.getCurrency());
            ps.setBoolean(7, plan.getActive());
            ps.setInt(8, plan.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Rimuove un piano se non ci sono sottoscrizioni attive/pending legate.
     * Ritorna true se cancellato; false se esistono sottoscrizioni o cancellazione fallita.
     */
    public boolean deletePlan(int planId) throws SQLException {
        String countSql = "SELECT COUNT(*) FROM subscription WHERE plan_id = ? AND status IN ('PENDING','ACTIVE')";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement cps = c.prepareStatement(countSql)) {
            cps.setInt(1, planId);
            try (ResultSet rs = cps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    LOG.log(Level.INFO, "Impossibile rimuovere piano {0}: esistono sottoscrizioni attive/pending", planId);
                    return false;
                }
            }
            String del = "DELETE FROM subscription_plan WHERE id = ?";
            try (PreparedStatement dps = c.prepareStatement(del)) {
                dps.setInt(1, planId);
                return dps.executeUpdate() > 0;
            }
        }
    }

    // ------------------- Sottoscrizioni -------------------

    /**
     * Crea una subscription (PENDING o altro stato) e ritorna id.
     */
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

    /**
     * Lista tutte le sottoscrizioni (admin).
     */
    public List<Subscription> listAllSubscriptions() throws SQLException {
        String sql = "SELECT id, user_id, plan_id, status, price_cents, currency, start_date, end_date, payment_provider, payment_provider_subscription_id FROM subscription ORDER BY start_date DESC, id DESC";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Subscription> out = new ArrayList<>();
            while (rs.next()) out.add(mapSubscription(rs));
            return out;
        }
    }

    /**
     * Ritorna la sottoscrizione attiva più recente per un utente (o null).
     */
    public Subscription getActiveSubscriptionForUser(int userId) throws SQLException {
        String sql = "SELECT id, user_id, plan_id, status, price_cents, currency, start_date, end_date, payment_provider, payment_provider_subscription_id FROM subscription WHERE user_id = ? AND status = 'ACTIVE' ORDER BY end_date DESC LIMIT 1";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapSubscription(rs);
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

    /**
     * Cerca sottoscrizioni che scadono entro la data fornita (inclusa).
     */
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

    /**
     * Marca una sottoscrizione come EXPIRED (se era ACTIVE).
     */
    public boolean markSubscriptionExpired(int subscriptionId) throws SQLException {
        String sql = "UPDATE subscription SET status = 'EXPIRED' WHERE id = ? AND status = 'ACTIVE'";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, subscriptionId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Annulla una sottoscrizione (CANCELLED) se è PENDING o ACTIVE.
     */
    public boolean cancelSubscription(int subscriptionId) throws SQLException {
        String sql = "UPDATE subscription SET status = 'CANCELLED' WHERE id = ? AND status IN ('PENDING','ACTIVE')";
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, subscriptionId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Attiva una sottoscrizione impostando start_date e end_date e lo stato ACTIVE.
     * Usa i valori passati.
     */
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

    /**
     * Overload: attiva la subscription calcolando automaticamente start/end in base al piano associato.
     * startDate = today
     * endDate = startDate + durationDays - 1 (compreso)
     */
    public boolean activateSubscription(int subscriptionId) throws SQLException {
        // Leggi subscription e piano
        String sql = "SELECT plan_id FROM subscription WHERE id = ?";
        Integer planId = null;
        try (Connection c = ConnectionPool.getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, subscriptionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) planId = rs.getInt("plan_id");
                else {
                    LOG.log(Level.WARNING, "Subscription id non trovato: {0}", subscriptionId);
                    return false;
                }
            }
        }

        SubscriptionPlan plan = findPlanById(planId);
        if (plan == null) {
            LOG.log(Level.WARNING, "Plan id non trovato durante attivazione subscription {0} -> planId={1}", new Object[]{subscriptionId, planId});
            return false;
        }

        LocalDate start = LocalDate.now();
        int duration = plan.getDurationDays() != null && plan.getDurationDays() > 0 ? plan.getDurationDays() : 30;
        // end inclusive: start + duration - 1 giorno
        LocalDate end = start.plusDays(duration - 1L);

        return activateSubscription(subscriptionId, start, end, null);
    }

}
