<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!doctype html>
<html>
<head><title>Staff - Admin</title></head>
<body>
<h1>Staff</h1>
<p><a href="${pageContext.request.contextPath}/admin/home">Admin Home</a></p>

<table border="1" cellpadding="6">
  <thead><tr><th>ID</th><th>Nome</th><th>Cognome</th><th>Email</th><th>Azioni</th></tr></thead>
  <tbody>
    <c:forEach var="s" items="${staffList}">
      <tr>
        <td>${s.id}</td>
        <td>${s.nome}</td>
        <td>${s.cognome}</td>
        <td>${s.email}</td>
        <td>
          <!-- ad esempio demote -> CLIENTE / promote -> PROPRIETARIO -->
          <form method="post" action="${pageContext.request.contextPath}/admin/users/action" style="display:inline">
            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
            <input type="hidden" name="action" value="changeRole"/>
            <input type="hidden" name="id" value="${s.id}"/>
            <select name="role">
              <option value="CLIENTE">CLIENTE</option>
              <option value="PERSONALE" selected>PERSONALE</option>
              <option value="PROPRIETARIO">PROPRIETARIO</option>
            </select>
            <button type="submit">Aggiorna</button>
          </form>
        </td>
      </tr>
    </c:forEach>
    <c:if test="${empty staffList}">
      <tr><td colspan="5">Nessun membro del personale</td></tr>
    </c:if>
  </tbody>
</table>
</body>
</html>
