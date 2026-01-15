<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="it.homegym.model.Utente" %>
<%
    Utente u = (Utente) session.getAttribute("user");
    if (u == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }
    if (!"PROPRIETARIO".equals(u.getRuolo())) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
    }
%>
<!doctype html>
<html>
<head><title>Admin - Home</title></head>
<body>
<h1>Admin Dashboard</h1>
<p>Benvenuto <strong><%= u.getNome() %> <%= u.getCognome() %></strong> (<%= u.getRuolo() %>)</p>

<ul>
  <li><a href="<%=request.getContextPath()%>/admin/users">Gestione utenti</a></li>
  <li><a href="<%=request.getContextPath()%>/admin/payments">Gestione pagamenti</a></li>
  <li><a href="<%=request.getContextPath()%>/staff/community">Community</a></li>
  <li><a href="<%=request.getContextPath()%>/admin/stats">Statistiche</a></li>
  <li><a href="<%=request.getContextPath()%>/logout">Logout</a></li>
</ul>
</body>
</html>
