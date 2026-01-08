package it.homegym.listener;

import it.homegym.security.RateLimitService;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppLifecycleListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // nothing (RateLimitService inizializzato staticamente)
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        RateLimitService.shutdown();
    }
}
