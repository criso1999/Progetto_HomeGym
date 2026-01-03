<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html>
<head><title>Registrazione</title></head>
<body>

<h2>Registrazione HomeGym</h2>

<c:if test="${not empty error}">
    <p style="color:red">${error}</p>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/register">
    <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
    <input name="nome" placeholder="Nome" required><br>
    <input name="cognome" placeholder="Cognome" required><br>
    <input name="email" type="email" placeholder="Email" required><br>
    <input name="password" type="password" placeholder="Password" required><br>
    <button type="submit">Registrati</button>
</form>

</body>
</html>
