<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
<head><title>Prenota sessione</title></head>
<body>
<h1>Prenota una sessione</h1>

<c:if test="${not empty error}"><div style="color:red">${error}</div></c:if>

<form method="post" action="${pageContext.request.contextPath}/client/sessions/action">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
  <input type="hidden" name="action" value="create"/>

  <label>Trainer:
    <select name="trainer" required>
      <option value="">-- seleziona --</option>
      <c:forEach var="t" items="${requestScope.trainers}">
        <option value="${t.nome} ${t.cognome}">${t.nome} ${t.cognome} (${t.email})</option>
      </c:forEach>
    </select>
  </label>

  <label>Data/ora:
    <input type="datetime-local" name="scheduled_at" value="${requestScope.scheduledAtInput}" required />
  </label>

  <label>Durata (min): <input type="number" name="duration" min="1" value="60"/></label>

  <label>Note:<br/><textarea name="notes" rows="4" cols="60"></textarea></label>

  <button type="submit">Prenota</button>
</form>

<p><a href="${pageContext.request.contextPath}/client/sessions">← Torna alle mie sessioni</a></p>
</body>
</html>
