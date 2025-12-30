<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="it.homegym.model.Utente" %>
<%
 Utente u = (Utente) session.getAttribute("proprietario");
 if (u == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }
%>
<html>
<head><title>Home</title></head>
<body>
<h2>Benvenuto, <%= u.getNome() %> <%= u.getCognome() %></h2>
<p>Ruolo: <%= u.getRuolo() %></p>
<p><a href="<%=request.getContextPath()%>/logout">Logout</a></p>
</body>
</html>
