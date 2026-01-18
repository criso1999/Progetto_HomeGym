<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="it.homegym.model.Utente" %>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Clienti - Staff</title>
  <style>
    .deleted { color: #999; text-decoration: line-through; }
  </style>
</head>
<body>
<h1>Clienti</h1>

<!-- Se sei PERSONALE: form per assegnare un cliente esistente a te -->
<c:if test="${not empty currentUser and currentUser.ruolo == 'PERSONALE'}">
  <h3>Aggiungi cliente esistente al tuo elenco</h3>
  <c:if test="${empty availableClients}">
    <p>Nessun cliente disponibile per l'assegnazione.</p>
  </c:if>
  <c:if test="${not empty availableClients}">
    <form method="post" action="${pageContext.request.contextPath}/staff/clients/action">
      <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
      <input type="hidden" name="action" value="assign"/>
      <label>
        Seleziona cliente:
        <select name="existingUserId" required>
          <c:forEach var="ac" items="${availableClients}">
            <option value="${ac.id}">${ac.nome} ${ac.cognome} — ${ac.email}</option>
          </c:forEach>
        </select>
      </label>
      <button type="submit">Aggiungi cliente</button>
    </form>
  </c:if>
  <hr/>
</c:if>

<!-- Se sei PROPRIETARIO: puoi assegnare qualsiasi cliente a un trainer (select trainer opzionale) -->
<c:if test="${not empty currentUser and currentUser.ruolo == 'PROPRIETARIO'}">
  <h3>Assegna cliente a un trainer (admin)</h3>
  <c:if test="${empty availableClients}">
    <p>Nessun cliente disponibile per l'assegnazione.</p>
  </c:if>
  <c:if test="${not empty availableClients}">
    <form method="post" action="${pageContext.request.contextPath}/staff/clients/action">
      <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
      <input type="hidden" name="action" value="assign"/>
      <label>
        Cliente:
        <select name="existingUserId" required>
          <c:forEach var="ac" items="${availableClients}">
            <option value="${ac.id}">${ac.nome} ${ac.cognome} — ${ac.email}</option>
          </c:forEach>
        </select>
      </label>

      <label>
        Trainer:
        <select name="trainerId">
          <option value="">-- seleziona trainer --</option>
          <c:forEach var="t" items="${trainers}">
            <option value="${t.id}">${t.nome} ${t.cognome} (${t.email})</option>
          </c:forEach>
        </select>
      </label>

      <button type="submit">Assegna</button>
    </form>
  </c:if>
  <hr/>
</c:if>

<table border="1" cellpadding="6">
  <thead>
    <tr><th>ID</th><th>Nome</th><th>Cognome</th><th>Email</th><th>Trainer</th><th>Azioni</th></tr>
  </thead>
  <tbody>
    <c:forEach var="c" items="${clients}">
      <tr class="${c.deleted ? 'deleted' : ''}">
        <td><c:out value="${c.id}"/></td>
        <td><c:out value="${c.nome}"/></td>
        <td><c:out value="${c.cognome}"/></td>
        <td><c:out value="${c.email}"/></td>
        <td>
          <c:choose>
            <c:when test="${not empty c.trainerId}">
              <c:out value="${c.trainerId}"/>
              <!-- se vuoi mostrare anche nome trainer, il servlet deve popolare una mappa id->nome -->
            </c:when>
            <c:otherwise>
              <em>—</em>
            </c:otherwise>
          </c:choose>
        </td>
        <td>
          <a href="${pageContext.request.contextPath}/staff/clients/form?id=${c.id}">Modifica</a>

          <form method="post" action="${pageContext.request.contextPath}/staff/clients/action" style="display:inline" onsubmit="return confirm('Confermi soft-delete di questo cliente?');">
            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
            <input type="hidden" name="action" value="delete"/>
            <input type="hidden" name="id" value="${c.id}"/>
            <button type="submit">Rimuovi (nascondi)</button>
          </form>
        </td>
      </tr>
    </c:forEach>

    <c:if test="${empty clients}">
      <tr><td colspan="6">Nessun cliente assegnato.</td></tr>
    </c:if>
  </tbody>
</table>

<p><a href="${pageContext.request.contextPath}/staff/home">← Torna</a></p>
</body>
</html>
