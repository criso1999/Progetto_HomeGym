package it.homegym.listener;

import it.homegym.dao.SubscriptionDAO;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

@WebListener
public class SubscriptionExpiryListener implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "subscription-expiry-check");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                SubscriptionDAO dao = new SubscriptionDAO();
                List<?> expiring = dao.listExpiringSubscriptions(LocalDate.now());
                for (Object o : expiring) {
                    // cast e marcatura expired
                    // dao.markSubscriptionExpired(subscriptionId);
                    // (invia mail notifiche se vuoi)
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.DAYS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
