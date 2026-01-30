<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Login - HomeGym</title>
  <script src="https://www.google.com/recaptcha/api.js" async defer></script>
  <style>
    body { font-family: Arial, sans-serif; max-width:700px; margin:24px auto; }
    .flash { padding:8px 12px; border-radius:6px; margin-bottom:12px; }
    .flash-success { background:#e6ffed; color:#0a6f3a; border:1px solid #c6f0d0; }
    .flash-error { background:#ffecec; color:#9b1b1b; border:1px solid #f3c6c6; }
    form { margin-top:12px; }
    label { display:block; margin-top:8px; font-weight:600; }
  </style>
</head>
<body>

<h2>Login</h2>

<c:if test="${not empty sessionScope.flashSuccess}">
  <div class="flash flash-success">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>

<c:if test="${not empty errorHtml}">
  <!-- errorHtml contiene markup (es. link per reinviare verifica) -->
  <div class="flash flash-error"><c:out value="${errorHtml}" escapeXml="false"/></div>
</c:if>

<c:if test="${empty errorHtml and not empty error}">
  <div class="flash flash-error">${error}</div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/login">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>

  <label for="email">Email</label>
  <input id="email" name="email" type="email" required value="${param.email != null ? param.email : ''}" />

  <label for="password">Password</label>
  <input id="password" name="password" type="password" required />

  <div style="margin-top:10px" class="g-recaptcha" data-sitekey="${recaptchaSiteKey}"></div>

  <div style="margin-top:12px;">
    <button type="submit">Entra</button>
  </div>
</form>

<p style="margin-top:12px">
  <a href="${pageContext.request.contextPath}/forgot">Hai dimenticato la password? Reimpostala qui</a>
  &nbsp;|&nbsp;
  <a href="${pageContext.request.contextPath}/register">Non hai un account? Registrati</a>
</p>

</body>
</html>
