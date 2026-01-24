<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Schede di allenamento</title>
  <style>
    table { border-collapse:collapse; width:100% }
    th,td { border:1px solid #ddd; padding:8px }
    th { background:#f5f5f5 }
    .small { color:#666; font-size:0.9em }
    .actions form { display:inline; margin:0 4px }
  </style>
</head>
<body>
<h1>Schede di allenamento</h1>

<p>
  <!-- CORRETTO: punta al form per creare/modificare -->
  <a href="${pageContext.request.contextPath}/staff/plans/form">➕ Nuova scheda</a>
  &nbsp;|&nbsp;
  <a href="${pageContext.request.contextPath}/staff/home">← Torna</a>
</p>

<c:if test="${not empty sessionScope.flashSuccess}">
  <div style="color:green">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div style="color:red">${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>

<table>
  <thead>
    <tr>
      <th>ID</th><th>Titolo</th><th>Creato da</th><th>Ult. aggiornamento</th><th>Allegato</th><th>Azioni</th>
    </tr>
  </thead>
  <tbody>
    <c:choose>
      <c:when test="${empty plans}">
        <tr><td colspan="6">Nessuna scheda trovata.</td></tr>
      </c:when>
      <c:otherwise>
        <c:forEach var="p" items="${plans}">
          <tr>
            <td><c:out value="${p.id}"/></td>
            <td><c:out value="${p.title}"/></td>
            <td><c:out value="${p.createdBy}"/></td>
            <td class="small"><c:out value="${p.updatedAt}"/></td>
            <td>
              <c:if test="${not empty p.attachmentFilename}">
                <a href="${pageContext.request.contextPath}/staff/plans/download?id=${p.id}">
                  <c:out value="${p.attachmentFilename}"/>
                </a>
              </c:if>
            </td>
            <td class="actions">
              <a href="${pageContext.request.contextPath}/staff/plans/form?id=${p.id}">Modifica</a>
              &nbsp;|&nbsp;
              <form method="post" action="${pageContext.request.contextPath}/staff/plans/action" style="display:inline">
                <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                <input type="hidden" name="action" value="delete"/>
                <input type="hidden" name="id" value="${p.id}"/>
                <button type="submit" onclick="return confirm('Confermi rimozione?')">Rimuovi</button>
              </form>

              &nbsp;|&nbsp;
              <a href="${pageContext.request.contextPath}/staff/plans/form?id=${p.id}#upload">Carica allegato</a>
              &nbsp;|&nbsp;
              
              <a href="${pageContext.request.contextPath}/staff/plans/assign?planId=${p.id}">Assegna</a>
              &nbsp;|&nbsp;
              <a href="${pageContext.request.contextPath}/staff/plans/history?id=${p.id}">Storico</a>
            </td>
          </tr>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </tbody>
</table>

</body>
</html>
