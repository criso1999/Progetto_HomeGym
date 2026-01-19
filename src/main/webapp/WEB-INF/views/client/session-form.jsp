<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Prenota sessione</title>
  <style>.small{color:#666;font-size:0.9em}</style>
</head>
<body>
<h1>Prenota una sessione con il trainer</h1>

<c:if test="${not empty error}"><div style="color:red">${error}</div></c:if>
<c:if test="${not empty sessionScope.flashError}"><div style="color:red">${sessionScope.flashError}</div><c:remove var="flashError" scope="session"/></c:if>
<c:if test="${not empty sessionScope.flashSuccess}"><div style="color:green">${sessionScope.flashSuccess}</div><c:remove var="flashSuccess" scope="session"/></c:if>

<form method="post" action="${pageContext.request.contextPath}/client/sessions/new">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>

  <label>Trainer:
    <select name="trainerId" required>
      <option value="">-- seleziona trainer --</option>
      <c:forEach var="t" items="${trainers}">
        <option value="${t.id}">${t.nome} ${t.cognome} (${t.email})</option>
      </c:forEach>
    </select>
  </label>
  <br/>

  <label>Data e ora:
    <!-- use datetime-local, browser provides picker -->
    <input type="datetime-local" name="scheduledAt" required />
  </label>
  <br/>

  <label>Durata (minuti):
    <input type="number" name="durationMinutes" min="15" step="15" value="60" />
  </label>
  <br/>

  <label>Note:
    <textarea name="notes" rows="4" cols="60"></textarea>
  </label>
  <br/>

  <label>Importo (EUR):
    <input type="number" name="amount" step="0.01" value="25.00" required />
    <span class="small">Inserisci l'importo per la sessione. Qui è simulato; in produzione collegare gateway.</span>
  </label>
  <br/>

  <button type="submit">Prenota e paga</button>
</form>

<p><a href="${pageContext.request.contextPath}/client/sessions">← Torna alle mie sessioni</a></p>
</body>
</html>
