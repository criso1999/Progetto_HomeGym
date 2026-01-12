<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="it.homegym.model.TrainingSession" %>

<%
    TrainingSession s = (TrainingSession) request.getAttribute("session");
%>

<!doctype html>
<html>
<head><title><c:out value="${s != null ? 'Modifica' : 'Nuova'}"/> sessione</title></head>
<body>
<h2><c:out value="${s != null ? 'Modifica' : 'Nuova'}"/> sessione</h2>

<form method="post" action="${pageContext.request.contextPath}/staff/sessions/action">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
  <input type="hidden" name="action" value="${s != null ? 'update' : 'create'}"/>
  <c:if test="${s != null}">
    <input type="hidden" name="id" value="${s.id}" />
  </c:if>

  <label>userId (opzionale): <input name="userId" value="${s != null ? s.userId : ''}" /></label><br/>
  <label>Trainer: <input name="trainer" required value="${s != null ? s.trainer : ''}" /></label><br/>
  <label>Data / Ora:
    <input type="datetime-local" name="scheduled_at"
      value="${s != null && s.when != null ? (s.when.toInstant().toString() /* fallback handled below */) : ''}" />
    <!-- Nota: potresti convertire Timestamp->yyyy-MM-dd'T'HH:mm nel servlet prima di settare l'attributo -->
  </label><br/>
  <label>Durata (min): <input name="duration" value="${s != null ? s.durationMinutes : '60'}" /></label><br/>
  <label>Note:<br/>
    <textarea name="notes">${s != null ? s.notes : ''}</textarea>
  </label><br/>

  <button type="submit">${s != null ? 'Aggiorna' : 'Crea'}</button>
</form>

<p><a href="${pageContext.request.contextPath}/staff/sessions">← Torna</a></p>
</body>
</html>
