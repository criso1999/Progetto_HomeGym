<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html><body>
<h1><c:out value="${plan != null ? 'Modifica scheda' : 'Nuova scheda'}"/></h1>

<form method="post" action="${pageContext.request.contextPath}/staff/plans/action" enctype="multipart/form-data">
  <input type="hidden" name="action" value="${plan != null ? 'update' : 'create'}"/>
  <c:if test="${plan != null}">
    <input type="hidden" name="id" value="${plan.id}"/>
  </c:if>

  Titolo: <input name="title" value="${plan.title}" required/><br/>
  Descrizione: <textarea name="description">${plan.description}</textarea><br/>
  Contenuto (json/text): <textarea name="content" rows="10" cols="80">${plan.content}</textarea><br/>

  <!-- UPLOAD -->
  <label>Allega scheda (PDF / Excel):</label>
  <input type="file" name="attachment" accept=".pdf,application/pdf,.xls,.xlsx,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" />
  <c:if test="${plan != null and not empty plan.attachmentFilename}">
    <div>Allegato corrente: <a href="${pageContext.request.contextPath}/staff/plans/download?id=${plan.id}">${plan.attachmentFilename}</a>
      ( <small>${plan.attachmentContentType}</small> )
    </div>
    <label><input type="checkbox" name="removeAttachment" value="1"/> Rimuovi allegato corrente</label>
  </c:if>

  <br/><br/>
  <button type="submit">Salva</button>
</form>
<p><a href="${pageContext.request.contextPath}/staff/plans">← Torna</a></p>
</body></html>
