<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title><c:out value="${plan != null ? 'Modifica scheda' : 'Nuova scheda'}"/></title>
  <style>
    .flash-success { color: green; margin-bottom: 8px; }
    .flash-error { color: red; margin-bottom: 8px; }
    .attachment { margin: 12px 0; }
  </style>
</head>
<body>

<h1><c:out value="${plan != null ? 'Modifica scheda' : 'Nuova scheda'}"/></h1>

<c:if test="${not empty sessionScope.flashSuccess}">
  <div class="flash-success">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div class="flash-error">${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>

<!-- Se la scheda esiste, mostro info e form upload -->
<c:if test="${plan != null}">
  <div>
    <strong>ID:</strong> <c:out value="${plan.id}"/> &nbsp;
    <strong>Titolo:</strong> <c:out value="${plan.title}"/>
  </div>

  <c:if test="${not empty plan.attachmentFilename}">
    <div class="attachment">
      <strong>Allegato corrente:</strong>
      <a href="${pageContext.request.contextPath}/staff/plans/download?id=${plan.id}" target="_blank">
        <c:out value="${plan.attachmentFilename}"/>
      </a>
      <c:if test="${not empty plan.attachmentSize}"> — <small><c:out value="${plan.attachmentSize}"/> bytes</small></c:if>
    </div>
  </c:if>

  <form method="post"
        action="${pageContext.request.contextPath}/staff/plans/action"
        enctype="multipart/form-data">
    <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
    <input type="hidden" name="action" value="upload"/>
    <input type="hidden" name="planId" value="${plan.id}"/>

    <label>Allega scheda (PDF / Excel):
      <input type="file" name="attachment"
             accept=".pdf,.xls,.xlsx,application/pdf,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" />
    </label>
    <button type="submit">Carica allegato</button>
  </form>

  <p>
    <a href="${pageContext.request.contextPath}/staff/plans">← Torna alle schede</a>
  </p>
</c:if>

<!-- Se la scheda non esiste, istruisco l'utente a salvarla prima -->
<c:if test="${plan == null}">
  <p>Per caricare un allegato devi prima salvare la scheda. Clicca <a href="${pageContext.request.contextPath}/staff/plans/form">qui</a> per creare la scheda, poi torna su questa pagina e carica l'allegato.</p>
</c:if>

</body>
</html>
