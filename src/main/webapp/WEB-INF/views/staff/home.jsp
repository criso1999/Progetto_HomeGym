<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="it.homegym.model.Utente" %>
<%
    Utente u = (Utente) session.getAttribute("user");
    if (u == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }
    if (!"PERSONALE".equals(u.getRuolo()) && !"PROPRIETARIO".equals(u.getRuolo())) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
    }
%>
<!doctype html>
<html>
<head><title>Staff - Home</title></head>
<body>
<h1>Staff Dashboard</h1>
<p>Benvenuto <%= u.getNome() %> <%= u.getCognome() %> (<<%= u.getRuolo() %>>)</p>

<ul>
  <li><a href="<%=request.getContextPath()%>/staff/clients">Clienti</a></li>
  <li><a href="<%=request.getContextPath()%>/staff/sessions">Sessioni</a></li>
  <li><a href="<%=request.getContextPath()%>/logout">Logout</a></li>
</ul>
</body>
</html>
