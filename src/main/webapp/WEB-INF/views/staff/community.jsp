<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Community - Trainer Feed</title>
  <style>
    .post { border:1px solid #ddd; padding:12px; margin:12px 0; }
    .media img, .media video { max-width:100%; display:block; margin:6px 0; }
    .small { color:#666; font-size:0.9em; }
  </style>
</head>
<body>
  <h1>Community — Posts dei tuoi clienti</h1>
  <p><a href="${pageContext.request.contextPath}/staff/home">← Staff Home</a></p>

  <c:if test="${empty posts}">
    <p>Nessun post trovato dai tuoi clienti.</p>
  </c:if>

  <c:forEach var="post" items="${posts}">
    <div class="post">
      <h3><c:out value="${post.userName}" /></h3>
      <div class="small"><fmt:formatDate value="${post.createdAt}" pattern="yyyy-MM-dd HH:mm" /></div>
      <p><c:out value="${post.content}" /></p>

      <c:if test="${not empty post.medias}">
        <div class="media">
          <c:forEach var="m" items="${post.medias}">
            <c:set var="fid" value="${m.fileId}" />
            <c:set var="ctype" value="${m.contentType}" />
            <c:choose>
              <c:when test="${fn:startsWith(ctype, 'image')}">
                <img src="${pageContext.request.contextPath}/media/${fid}" alt="${m.filename}" />
              </c:when>
              <c:when test="${fn:startsWith(ctype, 'video')}">
                <video controls>
                  <source src="${pageContext.request.contextPath}/media/${fid}" type="${ctype}" />
                </video>
              </c:when>
              <c:otherwise>
                <a href="${pageContext.request.contextPath}/media/${fid}"><c:out value="${m.filename}" /></a>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </div>
      </c:if>

      <p>
        <a href="${pageContext.request.contextPath}/posts/view?id=${post._idStr}">View</a>
        &nbsp;|&nbsp;
        <a href="${pageContext.request.contextPath}/posts/view?id=${post._idStr}#comments">Comments</a>
      </p>

      <!-- evaluation form (solo per trainer) -->
      <c:if test="${not empty sessionScope.user and (sessionScope.user.ruolo == 'PERSONALE' or sessionScope.user.ruolo == 'PROPRIETARIO')}">
        <form method="post" action="${pageContext.request.contextPath}/posts/evaluate">
          <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
          <input type="hidden" name="postId" value="${post._idStr}" />
          Score:
          <select name="score" required>
            <option value="1">1</option>
            <option value="2">2</option>
            <option value="3" selected>3</option>
            <option value="4">4</option>
            <option value="5">5</option>
          </select>
          <input type="text" name="note" placeholder="Short note (optional)" />
          <button type="submit">Valuta</button>
        </form>
      </c:if>

    </div>
  </c:forEach>

</body>
</html>
