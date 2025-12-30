package it.homegym.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

public class ConnectionPool {

    private static DataSource dataSource;

    static {
        try {
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/ProgettoDB");
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Errore inizializzazione DataSource: " + e.getMessage());
        }
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}
