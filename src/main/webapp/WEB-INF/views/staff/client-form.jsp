<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="it.homegym.model.Utente" %>

<c:set var="client" value="${requestScope.client}" />
<%
  Utente current = (Utente) session.getAttribute("user");
%>

<!doctype html>
<html>
<head><title><c:out value="${client != null ? 'Modifica cliente' : 'Nuovo cliente'}"/></title></head>
<body>
<h1><c:out value="${client != null ? 'Modifica cliente' : 'Nuovo cliente'}"/></h1>

<!-- Se il trainer è loggato: permetti di assegnare un utente esistente -->
<c:if test="${not empty current and current.ruolo == 'PERSONALE'}">
  <h3>Assegna un cliente esistente a te (trainer)</h3>
  <c:if test="${empty existingClients}">
    <p>Nessun cliente esistente trovato.</p>
  </c:if>
  <c:if test="${not empty existingClients}">
    <form method="post" action="${pageContext.request.contextPath}/staff/clients/action">
      <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
      <input type="hidden" name="action" value="assign"/>
      <label>Seleziona cliente:
        <select name="existingUserId" required>
          <c:forEach var="ec" items="${existingClients}">
            <option value="${ec.id}"><c:out value="${ec.nome}"/> <c:out value="${ec.cognome}"/> — ${ec.email}</option>
          </c:forEach>
        </select>
      </label>
      <!-- trainerId implicito: current.id (server side) -->
      <button type="submit">Assegna a me</button>
    </form>
  </c:if>

  <hr/>
  <p>Oppure crea un nuovo cliente (verrà assegnato al trainer selezionato oppure lascialo vuoto):</p>
</c:if>

<!-- Form di creazione/modifica (visibile a tutti i ruoli abilitati) -->
<form method="post" action="${pageContext.request.contextPath}/staff/clients/action">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>

  <c:if test="${client != null}">
    <input type="hidden" name="action" value="update"/>
    <input type="hidden" name="id" value="${client.id}"/>
  </c:if>
  <c:if test="${client == null}">
    <input type="hidden" name="action" value="create"/>
  </c:if>

  Nome: <input name="nome" value="${client != null ? client.nome : ''}" required/><br/>
  Cognome: <input name="cognome" value="${client != null ? client.cognome : ''}" required/><br/>
  Email: <input name="email" type="email" value="${client != null ? client.email : ''}" required/><br/>

  <!-- Password solo per creazione (o cambiamento esplicito) -->
  <c:if test="${client == null}">
    Password (iniziale): <input name="password" type="password" required/><br/>
  </c:if>
  <c:if test="${client != null}">
    Nuova password (se vuoi cambiarla): <input name="password" type="password"/><br/>
  </c:if>

  <!-- Select trainer: se sei PERSONALE, imposta implicitamente se non specificato -->
  <label>Trainer (opzionale):
    <select name="trainerId">
      <option value="">-- nessuno --</option>
      <c:forEach var="t" items="${trainers}">
        <option value="${t.id}" <c:if test="${client != null and client.trainerId == t.id}">selected</c:if>>
          <c:out value="${t.nome}"/> <c:out value="${t.cognome}"/>
        </option>
      </c:forEach>
    </select>
  </label><br/>

  <button type="submit">Salva</button>
</form>

<p><a href="${pageContext.request.contextPath}/staff/clients">← Torna alla lista</a></p>
</body>
</html>
