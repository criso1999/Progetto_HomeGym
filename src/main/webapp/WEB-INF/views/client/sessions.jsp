<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!doctype html>
<html>
<head><title>Le mie sessioni</title></head>
<body>
<h1>Le mie sessioni</h1>

<c:if test="${not empty error}"><div style="color:red">${error}</div></c:if>

<p><a href="${pageContext.request.contextPath}/client/sessions/new">+ Prenota nuova sessione</a> | <a href="${pageContext.request.contextPath}/client/home">Home</a></p>

<table border="1" cellpadding="6">
  <thead><tr><th>ID</th><th>Trainer</th><th>Data/Ora</th><th>Durata</th><th>Note</th><th>Azioni</th></tr></thead>
  <tbody>
    <c:choose>
      <c:when test="${empty sessions}">
        <tr><td colspan="6">Nessuna prenotazione</td></tr>
      </c:when>
      <c:otherwise>
        <c:forEach var="s" items="${sessions}">
          <tr>
            <td><c:out value="${s.id}"/></td>
            <td><c:out value="${s.trainer}"/></td>
            <td><fmt:formatDate value="${s.when}" pattern="yyyy-MM-dd HH:mm"/></td>
            <td><c:out value="${s.durationMinutes}"/> min</td>
            <td><c:out value="${s.notes}"/></td>
            <td>
              <form method="post" action="${pageContext.request.contextPath}/client/sessions/action" onsubmit="return confirm('Confermi cancellazione?');">
                <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                <input type="hidden" name="action" value="cancel"/>
                <input type="hidden" name="id" value="${s.id}" />
                <button type="submit">Annulla</button>
              </form>
            </td>
          </tr>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </tbody>
</table>
</body>
</html>
