<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head><title>Registrazione</title></head>
<body>
<h2>Registrazione HomeGym</h2>

<c:if test="${not empty error}">
    <p style="color:red"><c:out value="${error}" /></p>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/register">  
    <input name="nome" placeholder="Nome" required><br>
    <input name="cognome" placeholder="Cognome" required><br>
    <input name="email" type="email" placeholder="Email" required><br>
    <input name="password" type="password" placeholder="Password" required><br>   
    <button type="submit">Registrati</button>
</form>

<p>Hai già un account? <a href="${pageContext.request.contextPath}/login">Login</a></p>
</body>
</html>
