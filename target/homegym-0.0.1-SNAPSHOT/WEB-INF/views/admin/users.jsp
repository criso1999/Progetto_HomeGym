<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List"%>
<%@ page import="it.homegym.model.Utente"%>
<%
    // controllo di sicurezza lato JSP (in aggiunta al filtro)
    Utente u = (Utente) session.getAttribute("user");
    if (u == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }
    if (!"PROPRIETARIO".equals(u.getRuolo())) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato");
        return;
    }

    List<Utente> utenti = (List<Utente>) request.getAttribute("utenti");
    if (utenti == null) {
        utenti = java.util.Collections.emptyList();
    }
%>
<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Gestione utenti - Admin</title>
  <style>
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ccc; padding: 6px; }
    th { background: #f0f0f0; }
    form.inline { display:inline; margin:0; padding:0; }
  </style>
</head>
<body>
  <h2>Gestione utenti</h2>
  <p>Benvenuto, <strong><%= u.getNome() %> <%= u.getCognome() %></strong> — <a href="<%=request.getContextPath()%>/admin/home">Admin Home</a> | <a href="<%=request.getContextPath()%>/logout">Logout</a></p>

  <table>
    <thead>
      <tr>
        <th>ID</th><th>Nome</th><th>Cognome</th><th>Email</th><th>Ruolo</th><th>Azioni</th>
      </tr>
    </thead>
    <tbody>
    <c:forEach var="it" items="${utenti}">
      <tr>
        <td>${it.id}</td>
        <td>${it.nome}</td>
        <td>${it.cognome}</td>
        <td>${it.email}</td>
        <td>${it.ruolo}</td>
        <td>
          <!-- Cambia ruolo -->
          <form class="inline" method="post" action="<%=request.getContextPath()%>/admin/users/action">
            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
            <input type="hidden" name="action" value="changeRole"/>
            <input type="hidden" name="id" value="${it.id}"/>
            <select name="role">
              <option value="CLIENTE" ${it.ruolo == 'CLIENTE' ? 'selected' : ''}>CLIENTE</option>
              <option value="PERSONALE" ${it.ruolo == 'PERSONALE' ? 'selected' : ''}>PERSONALE</option>
              <option value="PROPRIETARIO" ${it.ruolo == 'PROPRIETARIO' ? 'selected' : ''}>PROPRIETARIO</option>
            </select>
            <button type="submit">Aggiorna</button>
          </form>

          <!-- Cancella utente -->
          <form class="inline" method="post" action="<%=request.getContextPath()%>/admin/users/action" onsubmit="return confirm('Confermi la cancellazione?');">
            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
            <input type="hidden" name="action" value="delete"/>
            <input type="hidden" name="id" value="${it.id}"/>
            <button type="submit">Elimina</button>
          </form>
        </td>
      </tr>
    </c:forEach>
    </tbody>
  </table>
</body>
</html>
