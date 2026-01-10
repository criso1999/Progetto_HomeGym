<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="it.homegym.model.Utente" %>

<!doctype html>
<html>
<head><title>Clienti - Staff</title></head>
<body>
<h1>Clienti</h1>
<p><a href="${pageContext.request.contextPath}/staff/clients/form">Crea nuovo cliente</a> | <a href="${pageContext.request.contextPath}/home">Home</a></p>

<table border="1" cellpadding="6">
  <thead>
    <tr><th>ID</th><th>Nome</th><th>Cognome</th><th>Email</th><th>Azioni</th></tr>
  </thead>
  <tbody>
    <c:forEach var="c" items="${clients}">
      <tr>
        <td><c:out value="${c.id}"/></td>
        <td><c:out value="${c.nome}"/></td>
        <td><c:out value="${c.cognome}"/></td>
        <td><c:out value="${c.email}"/></td>
        <td>
          <a href="${pageContext.request.contextPath}/staff/clients/form?id=${c.id}">Modifica</a>
          <form method="post" action="${pageContext.request.contextPath}/staff/clients/action" style="display:inline" onsubmit="return confirm('Confermi?');">
            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
            <input type="hidden" name="action" value="delete"/>
            <input type="hidden" name="id" value="${c.id}"/>
            <button type="submit">Elimina</button>
          </form>
        </td>
      </tr>
    </c:forEach>
    <c:if test="${empty clients}">
      <tr><td colspan="5">Nessun cliente</td></tr>
    </c:if>
  </tbody>
</table>
</body>
</html>
