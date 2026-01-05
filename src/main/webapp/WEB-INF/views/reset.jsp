<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
<head><title>Reset password</title></head>
<body>
<h2>Reset password</h2>

<c:if test="${not empty error}">
  <div style="color:red">${error}</div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/reset">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
  <input type="hidden" name="token" value="${param.token}" />
  Nuova password: <input type="password" name="password" required /><br/>
  Conferma: <input type="password" name="password2" required /><br/>
  <button type="submit">Aggiorna password</button>
</form>
</body>
</html>
