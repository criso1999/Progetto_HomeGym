<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Visualizza scheda</title>
  <style>
    .meta { color:#666; font-size:0.95em; margin-bottom:10px }
    .content pre { white-space:pre-wrap; background:#f8f8f8; padding:12px; border:1px solid #eee }
    table { border-collapse:collapse; width:100%; margin-top:12px }
    th,td { border:1px solid #ddd; padding:6px }
    th { background:#f5f5f5 }
  </style>
</head>
<body>
  <h1>Visualizza scheda</h1>

  <c:if test="${not empty sessionScope.flashError}">
    <div style="color:red">${sessionScope.flashError}</div>
    <c:remove var="flashError" scope="session"/>
  </c:if>

  <p><a href="${pageContext.request.contextPath}/client/plans">← Torna alle mie schede</a></p>

  <c:if test="${not empty plan}">
    <h2><c:out value="${plan.title}"/></h2>

    <div class="meta">
      <c:if test="${not empty trainer}">
        Trainer: <strong><c:out value="${trainer.nome}"/> <c:out value="${trainer.cognome}"/></strong>
        &nbsp;—&nbsp;<c:out value="${trainer.email}"/><br/>
      </c:if>
      Assegnata il: <fmt:formatDate value="${assignment.assignedAt}" pattern="yyyy-MM-dd HH:mm"/>
      &nbsp;|&nbsp;Stato: <c:out value="${assignment.active ? 'Attiva' : 'Non attiva'}"/>
    </div>

    <c:if test="${not empty plan.description}">
      <p><strong>Descrizione</strong><br/><c:out value="${plan.description}"/></p>
    </c:if>

    <div class="content">
      <h3>Contenuto</h3>
      <c:choose>
        <c:when test="${not empty plan.content}">
          <pre><c:out value="${plan.content}"/></pre>
        </c:when>
        <c:otherwise>
          <p><em>Contenuto non disponibile.</em></p>
        </c:otherwise>
      </c:choose>
    </div>

    <c:if test="${not empty plan.attachmentPath or not empty plan.attachmentFilename}">
      <div style="margin-top:10px">
        <strong>Allegato:</strong>

        <c:choose>
          <c:when test="${not empty plan.attachmentFilename}">
            <c:set var="displayName" value="${plan.attachmentFilename}" />
          </c:when>
          <c:otherwise>
            <c:set var="displayName" value="${plan.attachmentPath}" />
          </c:otherwise>
        </c:choose>

        <c:choose>
          <c:when test="${not empty sessionScope.user and sessionScope.user.ruolo == 'CLIENTE'}">
            <c:url var="downloadUrl" value="/client/plans/download">
              <c:param name="planId" value="${plan.id}" />
              <c:if test="${not empty assignment}">
                <c:param name="assignmentId" value="${assignment.id}" />
              </c:if>
            </c:url>
            <a href="${downloadUrl}" target="_blank" rel="noopener noreferrer">
              <c:out value="${displayName}"/>
            </a>
          </c:when>
          <c:otherwise>
            <c:url var="downloadUrlStaff" value="/staff/plans/download">
              <c:param name="id" value="${plan.id}" />
            </c:url>
            <a href="${downloadUrlStaff}" target="_blank" rel="noopener noreferrer">
              <c:out value="${displayName}"/>
            </a>
          </c:otherwise>
        </c:choose>

        <c:if test="${not empty plan.attachmentPath}">
          &nbsp;|&nbsp;
          <a href="${pageContext.request.contextPath}${plan.attachmentPath}" target="_blank" rel="noopener noreferrer">(apri percorso)</a>
        </c:if>
      </div>
    </c:if>

    <h3 style="margin-top:16px">Storico versioni</h3>
    <c:if test="${empty history}">
      <p>Nessuna versione precedente disponibile.</p>
    </c:if>
    <c:if test="${not empty history}">
      <table>
        <thead><tr><th>Versione</th><th>Titolo</th><th>Creato da (id)</th><th>Data</th></tr></thead>
        <tbody>
          <c:forEach var="v" items="${history}">
            <tr>
              <td><c:out value="${v.versionNumber}"/></td>
              <td><c:out value="${v.title}"/></td>
              <td><c:out value="${v.createdBy}"/></td>
              <td><fmt:formatDate value="${v.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:if>

  </c:if>

</body>
</html>
