<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
<head><title>Profilo</title></head>
<body>
<h1>Il mio profilo</h1>

<c:if test="${not empty info}"><div style="color:green">${info}</div></c:if>
<c:if test="${not empty error}"><div style="color:red">${error}</div></c:if>

<form method="post" action="${pageContext.request.contextPath}/client/profile">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
  Nome: <input name="nome" value="${sessionScope.user.nome}" required /><br/>
  Cognome: <input name="cognome" value="${sessionScope.user.cognome}" required /><br/>
  Email: <input name="email" value="${sessionScope.user.email}" type="email" required /><br/>
  Nuova password (lascia vuoto per mantenere): <input name="password" type="password" /><br/>
  Conferma password: <input name="password2" type="password" /><br/>
  <button type="submit">Salva</button>
</form>

<p><a href="${pageContext.request.contextPath}/client/home">← Torna</a></p>
</body>
</html>
