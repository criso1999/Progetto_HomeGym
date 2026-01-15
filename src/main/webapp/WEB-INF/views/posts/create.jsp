<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html>
<head><title>Create post</title></head>
<body>
  <h1>Create Post</h1>
  <form method="post" action="${pageContext.request.contextPath}/posts/create" enctype="multipart/form-data">
    <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
    <textarea name="content" rows="6" cols="60" placeholder="Write about your training..."></textarea><br/>
    <label>Media (image/video) <input type="file" name="media" multiple /></label><br/>
    <button type="submit">Post</button>
  </form>
  <c:if test="${sessionScope.user.ruolo == 'PROPRIETARIO'}">
    <p><a href="${pageContext.request.contextPath}/admin/home">← Torna</a></p>
  </c:if>

  <c:if test="${sessionScope.user.ruolo == 'PERSONALE'}">
    <p><a href="${pageContext.request.contextPath}/staff/home">← Torna</a></p>
  </c:if>

  <c:if test="${sessionScope.user.ruolo == 'CLIENTE'}">
    <p><a href="${pageContext.request.contextPath}/client/home">← Torna</a></p>
  </c:if>

  <c:if test="${sessionScope.user.ruolo == 'CLIENTE'}">
    <p><a href="${pageContext.request.contextPath}/posts">← Feed</a></p>
  </c:if>
  
  <c:if test="${sessionScope.user.ruolo == 'PROPRIETARIO' || sessionScope.user.ruolo == 'PERSONALE'}">
    <p><a href="${pageContext.request.contextPath}/staff/community">← Feed</a></p>
  </c:if>
</body>
</html>
