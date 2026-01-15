<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>View Post</title>
  <style>
    .post { border:1px solid #ddd; padding:12px; margin:12px 0; }
    .media img, .media video { max-width:100%; display:block; margin:6px 0; }
    .comments { margin-top:18px; }
    .comment { border-top:1px solid #eee; padding:8px 0; }
    .eval { border-top:1px dashed #ddd; padding:8px 0; margin-top:12px; }
    .small { color:#666; font-size:0.9em; }
    form.inline { display:inline; margin:0; padding:0; }
  </style>
</head>
<body>

<c:if test="${not empty sessionScope.flashSuccess}">
  <div style="color:green">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div style="color:red">${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>

<c:choose>
  <c:when test="${empty post}">
    <h2>Post non trovato</h2>
    <c:if test="${sessionScope.user.ruolo == 'PROPRIETARIO' || sessionScope.user.ruolo == 'PERSONALE'}">
      <p><a href="${pageContext.request.contextPath}/staff/community">← Back to feed</a></p>
    </c:if>
    <c:if test="${sessionScope.user.ruolo == 'CLIENTE'}">
      <p><a href="${pageContext.request.contextPath}/posts">← Back to feed</a></p>
    </c:if>
  </c:when>

  <c:otherwise>
    <div class="post">
      <h2><c:out value="${post.userName}" /></h2>
      <div class="small">
        <fmt:formatDate value="${post.createdAt}" pattern="yyyy-MM-dd HH:mm:ss" />
      </div>

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
                  Your browser does not support the video tag.
                </video>
              </c:when>
              <c:otherwise>
                <a href="${pageContext.request.contextPath}/media/${fid}"><c:out value="${m.filename}" /></a>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </div>
      </c:if>
      <!-- DELETE POST OPTION -->
      <c:if test="${not empty sessionScope.user and 
            (sessionScope.user.ruolo == 'PROPRIETARIO' 
             or sessionScope.user.id == post.userId)}">

      <form method="post"
            action="${pageContext.request.contextPath}/posts/delete"
            onsubmit="return confirm('Vuoi eliminare definitivamente questo post?');"
            class="inline">

        <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
        <input type="hidden" name="postId" value="${post._idStr}" />
        <button type="submit" style="color:red">🗑 Elimina</button>
      </form>

    </c:if>


      <p class="small">Visibility: <c:out value="${post.visibility}" /></p>

      <p>
        <a href="${pageContext.request.contextPath}/posts">← Back to feed</a>
        <c:if test="${not empty sessionScope.user}">
          &nbsp;|&nbsp;<a href="${pageContext.request.contextPath}/posts/view?id=${post._idStr}#comments">Jump to comments</a>
        </c:if>
      </p>
    </div>

    <!-- COMMENTS -->
    <div id="comments" class="comments">
      <h3>Comments (<c:out value="${fn:length(post.comments)}"/>)</h3>

      <c:if test="${empty post.comments}">
        <p>No comments yet.</p>
      </c:if>

      <c:forEach var="cm" items="${post.comments}">
        <div class="comment">
          <strong><c:out value="${cm.userName}"/></strong>
          <span class="small"> — <fmt:formatDate value="${cm.createdAt}" pattern="yyyy-MM-dd HH:mm" /></span>
          <p><c:out value="${cm.text}"/></p>
        </div>
      </c:forEach>

      <c:if test="${not empty sessionScope.user}">
        <h4>Add a comment</h4>
        <form method="post" action="${pageContext.request.contextPath}/posts/comment">
          <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
          <input type="hidden" name="postId" value="${post._idStr}" />
          <textarea name="text" rows="4" cols="60" required placeholder="Write your comment..."></textarea><br/>
          <button type="submit">Submit comment</button>
        </form>
      </c:if>

      <c:if test="${empty sessionScope.user}">
        <p><a href="${pageContext.request.contextPath}/login">Log in</a> to comment.</p>
      </c:if>
    </div>

    <!-- TRAINER EVALUATIONS -->
    <div class="eval">
      <h3>Trainer evaluations (<c:out value="${fn:length(post.trainerEvaluations)}"/>)</h3>

      <c:if test="${empty post.trainerEvaluations}">
        <p>No evaluations yet.</p>
      </c:if>

      <c:forEach var="ev" items="${post.trainerEvaluations}">
        <div class="comment">
          <strong><c:out value="${ev.trainerName}"/></strong>
          <span class="small"> — score: <c:out value="${ev.score}"/></span>
          <div><c:out value="${ev.note}"/></div>
          <div class="small"><fmt:formatDate value="${ev.createdAt}" pattern="yyyy-MM-dd HH:mm" /></div>
        </div>
      </c:forEach>

      <c:if test="${not empty sessionScope.user and (sessionScope.user.ruolo == 'PERSONALE' or sessionScope.user.ruolo == 'PROPRIETARIO')}">
        <h4>Leave an evaluation</h4>
        <form method="post" action="${pageContext.request.contextPath}/posts/evaluate">
          <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
          <input type="hidden" name="postId" value="${post._idStr}" />
          <label>Score:
            <select name="score" required>
              <option value="1">1 - poor</option>
              <option value="2">2</option>
              <option value="3">3 - average</option>
              <option value="4">4</option>
              <option value="5" selected>5 - excellent</option>
            </select>
          </label><br/>
          <label>Note (optional):<br/>
            <textarea name="note" rows="4" cols="60" placeholder="Add notes for the trainee..."></textarea>
          </label><br/>
          <button type="submit">Submit evaluation</button>
        </form>
      </c:if>

      <c:if test="${empty sessionScope.user or (sessionScope.user.ruolo != 'PERSONALE' and sessionScope.user.ruolo != 'PROPRIETARIO')}">
        <p class="small">Only trainers can add evaluations.</p>
      </c:if>
    </div>

  </c:otherwise>
</c:choose>

</body>
</html>
