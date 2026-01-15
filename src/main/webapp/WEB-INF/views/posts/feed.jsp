<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!doctype html>
<html>
<head>
  <title>Community Feed</title>
  <style>
    .post { border:1px solid #ccc;padding:10px;margin:10px 0; }
    .hidden { color:red;font-weight:bold; }
  </style>
</head>
<body>

<h1>Community Feed</h1>
<p><a href="${pageContext.request.contextPath}/posts/create">➕ Nuovo Post</a></p>

<c:forEach var="p" items="${posts}">
  <div class="post">

    <c:if test="${p.visibility == 'HIDDEN'}">
      <div class="hidden">[POST NASCOSTO]</div>
    </c:if>

    <strong><c:out value="${p.userName}"/></strong>
    <span style="color:#666"> – <c:out value="${p.createdAt}"/></span>

    <p><c:out value="${p.content}"/></p>

    <!-- MEDIA -->
    <c:if test="${not empty p.medias}">
      <c:forEach var="m" items="${p.medias}">
        <c:set var="fid" value="${m.fileId}"/>
        <c:choose>
          <c:when test="${m.contentType.startsWith('image')}">
            <img src="${pageContext.request.contextPath}/media/${fid}" style="max-width:300px"/>
          </c:when>
          <c:when test="${m.contentType.startsWith('video')}">
            <video controls style="max-width:400px">
              <source src="${pageContext.request.contextPath}/media/${fid}" type="${m.contentType}"/>
            </video>
          </c:when>
          <c:otherwise>
            <a href="${pageContext.request.contextPath}/media/${fid}">
              <c:out value="${m.filename}"/>
            </a>
          </c:otherwise>
        </c:choose>
      </c:forEach>
    </c:if>

    <p>
      <a href="${pageContext.request.contextPath}/posts/view?id=${p._idStr}">
        🔍 Visualizza
      </a>
    </p>

    <!-- ADMIN DELETE -->
    <c:if test="${sessionScope.user.ruolo == 'PROPRIETARIO'}">
      <form method="post"
            action="${pageContext.request.contextPath}/posts/delete"
            onsubmit="return confirm('Eliminare definitivamente?');">
        <input type="hidden" name="postId" value="${p._idStr}"/>
        <button type="submit" style="color:red">🗑 Elimina</button>
      </form>
    </c:if>

  </div>
</c:forEach>

<!-- NAV -->
<c:choose>
  <c:when test="${sessionScope.user.ruolo == 'PROPRIETARIO'}">
    <a href="${pageContext.request.contextPath}/admin/home">← Admin Home</a>
  </c:when>
  <c:when test="${sessionScope.user.ruolo == 'PERSONALE'}">
    <a href="${pageContext.request.contextPath}/staff/home">← Staff Home</a>
  </c:when>
  <c:otherwise>
    <a href="${pageContext.request.contextPath}/client/home">← Home</a>
  </c:otherwise>
</c:choose>

</body>
</html>
