<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
<head><title>Reset password - Richiesta</title></head>
<body>
<h2>Richiedi reset password</h2>

<c:if test="${not empty info}">
  <div style="color:green">${info}</div>
</c:if>
<c:if test="${not empty error}">
  <div style="color:red">${error}</div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/forgot">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
  Email: <input type="email" name="email" required /><br/>
  <button type="submit">Invia link di reset</button>
</form>

<script src="https://www.google.com/recaptcha/api.js" async defer></script>
 
<div class="g-recaptcha" data-sitekey="${recaptchaSiteKey}"></div>
 
<p><a href="${pageContext.request.contextPath}/login">← Torna al login</a></p>
</body>
</html>
