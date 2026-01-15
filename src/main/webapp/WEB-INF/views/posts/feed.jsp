<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="org.bson.Document,org.bson.types.ObjectId" %>

<!doctype html>
<html>
<head><title>Feed</title></head>
<body>
  <h1>Community Feed</h1>
  <p><a href="${pageContext.request.contextPath}/posts/create">New Post</a></p>

  <c:forEach var="p" items="${requestScope.posts}">
    <div style="border:1px solid #ccc;padding:8px;margin:8px 0;">
      <strong><c:out value="${p.userName}"/></strong>
      <span style="color:#666"> - <c:out value="${p.createdAt}"/></span>
      <p><c:out value="${p.content}"/></p>

      <c:if test="${not empty p.medias}">
        <c:forEach var="m" items="${p.medias}">
          <c:set var="fid" value="${m.fileId}"/>
          <c:choose>
            <c:when test="${m.contentType.startsWith('image')}">
              <img src="${pageContext.request.contextPath}/media/${fid}" style="max-width:300px;display:block;margin:6px 0;"/>
            </c:when>
            <c:when test="${m.contentType.startsWith('video')}">
              <video controls style="max-width:400px;display:block;margin:6px 0;">
                <source src="${pageContext.request.contextPath}/media/${fid}" type="${m.contentType}">
                Your browser does not support the video tag.
              </video>
            </c:when>
            <c:otherwise>
              <a href="${pageContext.request.contextPath}/media/${fid}">${m.filename}</a>
            </c:otherwise>
          </c:choose>
        </c:forEach>
      </c:if>

      <p><a href="${pageContext.request.contextPath}/posts/view?id=${p._id}">View post</a></p>
    </div>
  </c:forEach>
  <p><a href="${pageContext.request.contextPath}/client/home">← Torna</a></p>
</body>
</html>
