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
    label { display:block; margin:8px 0; }
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

<form method="post"
      action="${pageContext.request.contextPath}/staff/plans/action"
      enctype="multipart/form-data">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>

  <!-- usiamo action create_and_upload: se non viene inviato file, viene comunque creata la scheda -->
  <input type="hidden" name="action" value="create_and_upload"/>
  <c:if test="${plan != null}">
    <input type="hidden" name="id" value="${plan.id}"/>
  </c:if>

  <label>Titolo
    <input type="text" name="title" value="${plan != null ? plan.title : ''}" required />
  </label>

  <label>Descrizione
    <textarea name="description" rows="3">${plan != null ? plan.description : ''}</textarea>
  </label>

  <label>Contenuto
    <textarea name="content" rows="8">${plan != null ? plan.content : ''}</textarea>
  </label>

  <hr/>

  <label>Allega scheda (opzionale — PDF/XLS/XLSX)
    <input type="file" name="attachment" accept=".pdf,.xls,.xlsx" />
  </label>

  <div style="margin-top:12px;">
    <button type="submit">Salva (e carica file se fornito)</button>
    <a href="${pageContext.request.contextPath}/staff/plans" style="margin-left:12px;">Annulla</a>
  </div>
</form>

<c:if test="${plan != null && not empty plan.attachmentFilename}">
  <div class="attachment">
    <strong>Allegato corrente:</strong>
    <a href="${pageContext.request.contextPath}/staff/plans/download?id=${plan.id}" target="_blank">
      <c:out value="${plan.attachmentFilename}"/>
    </a>
    <c:if test="${not empty plan.attachmentSize}"> — <small><c:out value="${plan.attachmentSize}"/> bytes</small></c:if>
  </div>
</c:if>

</body>
</html>
