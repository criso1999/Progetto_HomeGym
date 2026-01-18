<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Community Feed</title>
  <style>
    .post { border:1px solid #ddd; padding:12px; margin:12px 0; }
    .media img, .media video { max-width:100%; display:block; margin:6px 0; }
    .small { color:#666; font-size:0.9em; }
    .hidden { color:red; font-weight:bold; }
    form.inline { display:inline; }
  </style>
</head>
<body>

<h1>Community Feed</h1>

<!-- Flash messages -->
<c:if test="${not empty sessionScope.flashSuccess}">
  <div style="color:green;margin:10px 0;">
    ${sessionScope.flashSuccess}
  </div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>

<c:if test="${not empty sessionScope.flashError}">
  <div style="color:red;margin:10px 0;">
    ${sessionScope.flashError}
  </div>
  <c:remove var="flashError" scope="session"/>
</c:if>

<!-- NAV -->
<c:choose>
  <c:when test="${sessionScope.user.ruolo == 'PROPRIETARIO'}">
    <p><a href="${pageContext.request.contextPath}/admin/home">← Admin Home</a></p>
    <p class="small">Puoi nascondere o eliminare i post della community.</p>
  </c:when>
  <c:otherwise>
    <p><a href="${pageContext.request.contextPath}/staff/home">← Staff Home</a></p>
    <p class="small">Puoi visualizzare e valutare i post dei tuoi clienti.</p>
  </c:otherwise>
</c:choose>

<c:if test="${empty posts}">
  <p>Nessun post disponibile.</p>
</c:if>

<c:forEach var="post" items="${posts}">
  <div class="post">

    <!-- VISIBILITÀ -->
    <c:if test="${post.visibility == 'HIDDEN'}">
      <div class="hidden">[POST NASCOSTO]</div>
    </c:if>

    <h3><c:out value="${post.userName}" /></h3>
    <div class="small">
      <fmt:formatDate value="${post.createdAt}" pattern="yyyy-MM-dd HH:mm" />
    </div>

    <p><c:out value="${post.content}" /></p>

    <!-- MEDIA -->
    <c:if test="${not empty post.medias}">
      <div class="media">
        <c:forEach var="m" items="${post.medias}">
          <c:set var="fid" value="${m.fileId}" />
          <c:choose>
            <c:when test="${fn:startsWith(m.contentType,'image')}">
              <img src="${pageContext.request.contextPath}/media/${fid}" />
            </c:when>
            <c:when test="${fn:startsWith(m.contentType,'video')}">
              <video controls>
                <source src="${pageContext.request.contextPath}/media/${fid}" type="${m.contentType}" />
              </video>
            </c:when>
            <c:otherwise>
              <a href="${pageContext.request.contextPath}/media/${fid}">
                <c:out value="${m.filename}" />
              </a>
            </c:otherwise>
          </c:choose>
        </c:forEach>
      </div>
    </c:if>

    <!-- ================= ADMIN ACTIONS ================= -->
    <c:if test="${sessionScope.user.ruolo == 'PROPRIETARIO'}">
      <c:choose>
        <c:when test="${post.visibility == 'HIDDEN'}">
          <!-- Ripristina (UNHIDE) -->
          <form method="post"
                action="${pageContext.request.contextPath}/admin/posts/restore"
                class="inline"
                onsubmit="return confirm('Ripristinare la visibilità di questo post?');">
            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
            <input type="hidden" name="postId" value="${post._idStr}" />
            <button type="submit">♻ Ripristina</button>
          </form>

          <!-- opzionalmente mostra anche elimina definitiva -->
          <form method="post"
                action="${pageContext.request.contextPath}/posts/delete"
                class="inline"
                onsubmit="return confirm('Eliminazione DEFINITIVA. Continuare?');">
            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
            <input type="hidden" name="postId" value="${post._idStr}" />
            <button type="submit" style="color:red">🗑 Elimina</button>
          </form>

        </c:when>
        <c:otherwise>
          <!-- SOFT DELETE (nascondi) per post pubblici -->
          <form method="post"
                action="${pageContext.request.contextPath}/admin/posts/hide"
                class="inline"
                onsubmit="return confirm('Nascondere questo post dalla community?');">
            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
            <input type="hidden" name="postId" value="${post._idStr}" />
            <input type="text" name="reason" placeholder="Motivo (opzionale)" />
            <button type="submit">👁 Nascondi</button>
          </form>

          <form method="post"
                action="${pageContext.request.contextPath}/posts/delete"
                class="inline"
                onsubmit="return confirm('Eliminazione DEFINITIVA. Continuare?');">
            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
            <input type="hidden" name="postId" value="${post._idStr}" />
            <button type="submit" style="color:red">🗑 Elimina</button>
          </form>
        </c:otherwise>
      </c:choose>

    </c:if>


    <!-- ================= TRAINER EVALUATION ================= -->
    <c:if test="${sessionScope.user.ruolo == 'PERSONALE'}">
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

        <input type="text" name="note" placeholder="Nota (opzionale)" />
        <button type="submit">Valuta</button>
      </form>
    </c:if>

  </div>
</c:forEach>

</body>
</html>
