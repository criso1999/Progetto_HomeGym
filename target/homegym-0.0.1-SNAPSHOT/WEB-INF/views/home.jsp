<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:if test="${empty user}">
    <c:redirect url="${pageContext.request.contextPath}/login"/>
</c:if>

<html>
<head><title>Home</title></head>
<body>
<h2>Benvenuto, <c:out value="${user.nome}" /> <c:out value="${user.cognome}" /></h2>
<p>Ruolo: <c:out value="${user.ruolo}" /></p>
<p><a href="${pageContext.request.contextPath}/logout">Logout</a></p>
</body>
</html>
