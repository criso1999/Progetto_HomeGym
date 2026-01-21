<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!doctype html>
<html>
<head><meta charset="utf-8"/><title>Assegna scheda</title></head>
<body>
<h1>Assegna scheda: <c:out value="${plan.title}"/></h1>

<c:if test="${not empty sessionScope.flashError}"><div style="color:red">${sessionScope.flashError}</div><c:remove var="flashError" scope="session"/></c:if>
<c:if test="${not empty sessionScope.flashSuccess}"><div style="color:green">${sessionScope.flashSuccess}</div><c:remove var="flashSuccess" scope="session"/></c:if>

<form method="post" action="${pageContext.request.contextPath}/staff/plans/assign">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
  <input type="hidden" name="planId" value="${plan.id}" />

  <label>Seleziona cliente:
    <select name="userId" required>
      <option value="">-- seleziona cliente --</option>
      <c:forEach var="c" items="${clients}">
        <option value="${c.id}">${c.nome} ${c.cognome} — ${c.email}</option>
      </c:forEach>
    </select>
  </label>
  <br/>

  <c:if test="${sessionScope.user.ruolo == 'PROPRIETARIO'}">
    <label>Trainer (opzionale — lascia vuoto per usare il trainer corrente):
      <select name="trainerId">
        <option value="">-- seleziona trainer --</option>
        <c:forEach var="t" items="${trainers}">
          <option value="${t.id}">${t.nome} ${t.cognome} (${t.email})</option>
        </c:forEach>
      </select>
    </label>
    <br/>
  </c:if>

  <label>Note (opzionali):<br/>
    <textarea name="notes" rows="4" cols="60"></textarea>
  </label>
  <br/><br/>

  <button type="submit">Assegna scheda</button>
</form>

<p><a href="${pageContext.request.contextPath}/staff/plans">← Torna alle schede</a></p>
</body>
</html>
