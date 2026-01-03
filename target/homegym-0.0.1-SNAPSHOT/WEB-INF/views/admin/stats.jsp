<%@ page contentType="text/html;charset=UTF-8" %>
<%
    it.homegym.dao.PaymentDAO.Stats stats = (it.homegym.dao.PaymentDAO.Stats) request.getAttribute("paymentStats");
    Integer totalUsers = (Integer) request.getAttribute("totalUsers");
    if (stats == null) {
        stats = new it.homegym.dao.PaymentDAO.Stats();
        stats.totalPayments = 0;
        stats.totalAmount = java.math.BigDecimal.ZERO;
    }
%>
<!doctype html>
<html>
<head><title>Admin - Statistiche</title></head>
<body>
<h1>Statistiche</h1>
<p><a href="<%=request.getContextPath()%>/admin/home">← Admin Home</a></p>

<ul>
    <li>Utenti registrati: <strong><%= totalUsers != null ? totalUsers : 0 %></strong></li>
    <li>Totale pagamenti: <strong><%= stats.totalPayments %></strong></li>
    <li>Fatturato (somma): <strong><%= stats.totalAmount %></strong></li>
</ul>

</body>
</html>
