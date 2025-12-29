package it.homegym.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/jnditest")
public class JndiTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain;charset=UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            Context initCtx = new InitialContext();
            DataSource ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/ProgettoDB");

            if (ds == null) {
                out.println("JNDI lookup returned null");
                return;
            }

            try (Connection conn = ds.getConnection()) {
                out.println("OK - DB connection acquired");
            }
        } catch (NamingException | SQLException e) {
            throw new ServletException(e);
        }
    }
}
