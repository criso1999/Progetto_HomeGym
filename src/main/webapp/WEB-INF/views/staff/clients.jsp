<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="it.homegym.model.Utente" %>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Clienti - Staff</title>
  <style>
    .deleted { color: #999; text-decoration: line-through; }
    .small { font-size:0.9em; color:#666; }
    form.inline { display:inline; }
  </style>
</head>
<body>
<c:if test="${not empty sessionScope.flashSuccess}">
  <div style="color:green">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div style="color:red">${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>

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
              <!-- preferisci nome del trainer se fornito dal servlet -->
              <c:choose>
                <c:when test="${not empty trainerNames and not empty trainerNames[c.trainerId]}">
                  <c:out value="${trainerNames[c.trainerId]}"/>
                </c:when>
                <c:otherwise>
                  <c:out value="${c.trainerId}"/>
                </c:otherwise>
              </c:choose>
            </c:when>
            <c:otherwise>
              <em>—</em>
            </c:otherwise>
          </c:choose>
        </td>
        <td>
          <a href="${pageContext.request.contextPath}/staff/clients/form?id=${c.id}">Modifica</a>
          &nbsp;

          <!-- Se il cliente è soft-deleted: mostra Ripristina (se autorizzato) oppure una label -->
          <c:if test="${c.deleted}">
            <c:choose>
              <c:when test="${sessionScope.user.ruolo == 'PROPRIETARIO'}">
                <form method="post" action="${pageContext.request.contextPath}/staff/clients/action" class="inline" onsubmit="return confirm('Ripristinare questo cliente?');">
                  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                  <input type="hidden" name="action" value="restore"/>
                  <input type="hidden" name="id" value="${c.id}"/>
                  <button type="submit">Ripristina</button>
                </form>
              </c:when>
              <c:when test="${sessionScope.user.ruolo == 'PERSONALE' and sessionScope.user.id == c.trainerId}">
                <form method="post" action="${pageContext.request.contextPath}/staff/clients/action" class="inline" onsubmit="return confirm('Ripristinare questo cliente?');">
                  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                  <input type="hidden" name="action" value="restore"/>
                  <input type="hidden" name="id" value="${c.id}"/>
                  <button type="submit">Ripristina</button>
                </form>
              </c:when>
              <c:otherwise>
                <span class="small">Rimosso</span>
              </c:otherwise>
            </c:choose>
          </c:if>

          <!-- Se non deleted: mostra il bottone Rimuovi (soft-delete) -->
          <c:if test="${not c.deleted}">
            <form method="post" action="${pageContext.request.contextPath}/staff/clients/action" class="inline" onsubmit="return confirm('Confermi soft-delete di questo cliente?');">
              <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
              <input type="hidden" name="action" value="delete"/>
              <input type="hidden" name="id" value="${c.id}"/>
              <button type="submit">Rimuovi (nascondi)</button>
            </form>
          </c:if>

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
