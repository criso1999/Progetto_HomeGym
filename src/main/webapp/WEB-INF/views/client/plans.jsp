<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Le mie schede assegnate</title>
  <style>
    table { border-collapse:collapse; width:100% }
    th,td { border:1px solid #ddd; padding:8px }
    th { background:#f5f5f5 }
    .small { color:#666; font-size:0.9em }
    .actions a, .actions form { margin-right:8px; }
  </style>
</head>
<body>
<h1>Le mie schede assegnate</h1>

<c:if test="${not empty sessionScope.flashSuccess}">
  <div style="color:green">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div style="color:red">${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>

<p><a href="${pageContext.request.contextPath}/client/home">← Torna</a></p>

<c:choose>
  <c:when test="${empty assignments}">
    <p>Non hai ancora nessuna scheda assegnata.</p>
  </c:when>
  <c:otherwise>
    <table>
      <thead>
        <tr>
          <th>Assegnazione</th><th>Scheda</th><th>Trainer</th><th>Assegnata il</th><th>Attiva</th><th>Azioni</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="a" items="${assignments}">
          <tr>
            <td>#<c:out value="${a.id}"/></td>

            <td>
              <c:set var="plan" value="${plansMap[a.planId]}"/>
              <c:if test="${not empty plan}">
                <strong><c:out value="${plan.title}"/></strong><br/>
                <small><c:out value="${plan.description}"/></small>
              </c:if>
              <c:if test="${empty plan}"><em>Scheda non trovata (id ${a.planId})</em></c:if>
            </td>

            <td>
              <c:set var="t" value="${trainersMap[a.trainerId]}"/>
              <c:if test="${not empty t}">
                <c:out value="${t.nome}"/> <c:out value="${t.cognome}"/><br/>
                <small><c:out value="${t.email}"/></small>
              </c:if>
              <c:if test="${empty t}"><em>Trainer non disponibile</em></c:if>
            </td>

            <td class="small"><fmt:formatDate value="${a.assignedAt}" pattern="yyyy-MM-dd HH:mm"/></td>
            <td><c:out value="${a.active ? 'Sì' : 'No'}"/></td>

            <td class="actions">
              <c:if test="${not empty plan and not empty plan.attachmentPath}">
                <a href="${pageContext.request.contextPath}${plan.attachmentPath}" target="_blank">Scarica allegato</a>
              </c:if>
              <c:if test="${not empty plan}">
                <a href="${pageContext.request.contextPath}/client/plans/view?planId=${plan.id}&assignmentId=${a.id}">Visualizza scheda</a>
                &nbsp;|&nbsp;
                <a href="${pageContext.request.contextPath}/staff/plans/history?id=${plan.id}">Storico</a>
              </c:if>
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </c:otherwise>
</c:choose>

</body>
</html>
