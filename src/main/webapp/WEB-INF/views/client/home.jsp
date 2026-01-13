<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
<head><title>Client Home</title></head>
<body>
<h1>Benvenuto, <c:out value="${sessionScope.user.nome}"/>!</h1>

<ul>
  <li><a href="${pageContext.request.contextPath}/client/profile">Il mio profilo</a></li>
  <li><a href="${pageContext.request.contextPath}/client/sessions">Le mie sessioni</a></li>
  <li><a href="${pageContext.request.contextPath}/posts/create">Create Post</a></li>
  <li><a href="${pageContext.request.contextPath}/logout">Logout</a></li>
</ul>

</body>
</html>
