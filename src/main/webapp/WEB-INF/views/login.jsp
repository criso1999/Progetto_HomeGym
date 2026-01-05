<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
<head><title>Login - HomeGym</title></head>
<body>
<h2>Login</h2>

<c:if test="${not empty sessionScope.flashSuccess}">
  <div style="color:green">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>

<c:if test="${not empty error}">
  <div style="color:red">${error}</div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/login">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
  Email: <input type="email" name="email" required /><br/>
  Password: <input type="password" name="password" required /><br/>
  <button type="submit">Entra</button>
</form>
<p>Non hai un account? <a href="${pageContext.request.contextPath}/register">Registrati</a></p>
</body>
</html>
