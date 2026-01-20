<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="it.homegym.model.TrainingSession" %>

<%
    // controllo lato JSP (ulteriore protezione)
    it.homegym.model.Utente user = (it.homegym.model.Utente) session.getAttribute("user");
    if (user == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }
    // consentito solo personale/proprietario o secondo le tue regole (AuthFilter già fa il grosso)
%>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Sessioni - Staff</title>
  <style>
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ddd; padding: 8px; }
    th { background: #f4f4f4; }
    .actions form { display:inline; margin:0; }
  </style>
</head>
<body>
  <h1>Gestione sessioni</h1>
  <p>Benvenuto, <strong>${sessionScope.user.nome} ${sessionScope.user.cognome}</strong>
     — <a href="${pageContext.request.contextPath}/staff/home">Staff Home</a></p>

  <p><a href="${pageContext.request.contextPath}/staff/sessions/new">+ Crea nuova sessione</a></p>

  <table>
    <thead>
      <tr>
        <th>ID</th>
        <th>Utente</th>
        <th>Trainer</th>
        <th>Data / Ora</th>
        <th>Durata (min)</th>
        <th>Note</th>
        <th>Azioni</th>
      </tr>
    </thead>
    <tbody>
      <c:choose>
        <c:when test="${empty requestScope.sessions}">
          <tr><td colspan="7">Nessuna sessione trovata.</td></tr>
        </c:when>
        <c:otherwise>
          <c:forEach var="s" items="${sessions}">
            <tr>
              <td><c:out value="${s.id}" /></td>
              <td>
                <c:choose>
                  <c:when test="${not empty s.userName}">
                    <c:out value="${s.userName}" />
                  </c:when>
                  <c:when test="${not empty s.userId}">
                    <a href="${pageContext.request.contextPath}/staff/clients/${s.userId}">${s.userId}</a>
                  </c:when>
                  <c:otherwise>-</c:otherwise>
                </c:choose>
              </td>
              <td><c:out value="${s.trainer}" /></td>
              <td><c:out value="${s.when}" /></td>
              <td><c:out value="${s.durationMinutes}" /></td>
              <td><c:out value="${s.notes}" /></td>
              <td class="actions">
                <!-- link/azioni: view/edit/delete (da implementare lato servlet/DAO) -->
                <!--a href="${pageContext.request.contextPath}/staff/sessions/view?id=${s.id}">View</a>-->
                |
                <a href="${pageContext.request.contextPath}/staff/sessions/edit?id=${s.id}">Edit</a>
                |
                <form method="post" action="${pageContext.request.contextPath}/staff/sessions/action" onsubmit="return confirm('Confermi cancellazione?');" style="display:inline;">
                  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                  <input type="hidden" name="action" value="delete"/>
                  <input type="hidden" name="id" value="${s.id}"/>
                  <button type="submit">Delete</button>
                </form>
              </td>
            </tr>
          </c:forEach>
        </c:otherwise>
      </c:choose>
    </tbody>
  </table>

  <p><a href="${pageContext.request.contextPath}/staff/home">← Torna allo staff dashboard</a></p>
</body>
</html>
