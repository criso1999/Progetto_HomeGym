<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%-- Sicurezza lato JSP (in aggiunta al filtro) --%>
<%@ page import="it.homegym.model.Utente" %>
<%
    Utente u = (Utente) session.getAttribute("user");
    if (u == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }
    if (!"PROPRIETARIO".equals(u.getRuolo())) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso negato");
        return;
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
  <p>Benvenuto, <strong><%= u.getNome() %> <%= u.getCognome() %></strong> — 
     <a href="${pageContext.request.contextPath}/admin/home">Admin Home</a> | 
     <a href="${pageContext.request.contextPath}/logout">Logout</a></p>

  <table>
    <thead>
      <tr>
        <th>ID</th><th>Nome</th><th>Cognome</th><th>Email</th><th>Ruolo</th><th>Azioni</th>
      </tr>
    </thead>
    <tbody>
      <c:forEach var="it" items="${utenti}">
        <tr>
          <td><c:out value="${it.id}" /></td>
          <td><c:out value="${it.nome}" /></td>
          <td><c:out value="${it.cognome}" /></td>
          <td><c:out value="${it.email}" /></td>
          <td><c:out value="${it.ruolo}" /></td>
          <td>
            <form class="inline" method="post" action="${pageContext.request.contextPath}/admin/users/action">
              <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
              <input type="hidden" name="action" value="changeRole"/>
              <input type="hidden" name="id" value="${it.id}"/>
              <select name="role">
                <option value="CLIENTE"><c:if test="${it.ruolo == 'CLIENTE'}">selected</c:if>CLIENTE</option>
                <option value="PERSONALE"><c:if test="${it.ruolo == 'PERSONALE'}">selected</c:if>PERSONALE</option>
                <option value="PROPRIETARIO"><c:if test="${it.ruolo == 'PROPRIETARIO'}">selected</c:if>PROPRIETARIO</option>
              </select>
              <button type="submit">Aggiorna</button>
            </form>

            <form class="inline" method="post" action="${pageContext.request.contextPath}/admin/users/action" onsubmit="return confirm('Confermi la cancellazione?');">
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
