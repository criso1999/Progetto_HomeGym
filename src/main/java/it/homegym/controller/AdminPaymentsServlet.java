package it.homegym.controller;

import it.homegym.dao.PaymentDAO;
import it.homegym.model.Payment;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/admin/payments")
public class AdminPaymentsServlet extends HttpServlet {

    private PaymentDAO dao;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            dao = new PaymentDAO();
        } catch (Exception e) {
            throw new ServletException("Impossibile inizializzare PaymentDAO", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int page = 1;
        int pageSize = 10; // default

        String pageParam = req.getParameter("page");
        String sizeParam = req.getParameter("pageSize");

        try {
            if (pageParam != null) page = Math.max(1, Integer.parseInt(pageParam));
        } catch (NumberFormatException ignored) {}

        try {
            if (sizeParam != null) {
                int s = Integer.parseInt(sizeParam);
                if (s > 0 && s <= 100) pageSize = s;
            }
        } catch (NumberFormatException ignored) {}

        int offset = (page - 1) * pageSize;

        try {
            int totalCount = dao.countAll();
            List<Payment> payments = dao.listPage(offset, pageSize);
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);

            req.setAttribute("payments", payments);
            req.setAttribute("page", page);
            req.setAttribute("pageSize", pageSize);
            req.setAttribute("totalPages", totalPages);
            req.setAttribute("totalCount", totalCount);

            req.getRequestDispatcher("/WEB-INF/views/admin/payments.jsp").forward(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
