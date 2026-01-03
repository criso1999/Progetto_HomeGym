package it.homegym.controller;

import it.homegym.dao.PaymentDAO;
import it.homegym.dao.PaymentDAO.Stats;
import it.homegym.dao.UtenteDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/admin/stats")
public class AdminStatsServlet extends HttpServlet {
    private PaymentDAO paymentDAO;
    private UtenteDAO utenteDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        paymentDAO = new PaymentDAO();
        utenteDAO = new UtenteDAO();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Stats ps = paymentDAO.stats();
            int totalUsers = utenteDAO.listAll().size();
            req.setAttribute("paymentStats", ps);
            req.setAttribute("totalUsers", totalUsers);
            req.getRequestDispatcher("/WEB-INF/views/admin/stats.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
