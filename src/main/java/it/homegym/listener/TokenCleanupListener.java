package it.homegym.listener;
 
import it.homegym.dao.PasswordResetDAO;
 
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.*;
 
@WebListener
public class TokenCleanupListener implements ServletContextListener {
 
    private ScheduledExecutorService scheduler;
 
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "token-cleanup");
            t.setDaemon(true);
            return t;
        });
        PasswordResetDAO dao = new PasswordResetDAO();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int removed = dao.deleteExpired();
                if (removed > 0) {
                    sce.getServletContext().log("PasswordReset cleanup removed: " + removed);
                }
            } catch (Throwable t) {
                sce.getServletContext().log("Error during password_reset cleanup", t);
            }
        }, 10, 60, TimeUnit.MINUTES); // first run dopo 10min, poi ogni 60min
    }
 
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
 