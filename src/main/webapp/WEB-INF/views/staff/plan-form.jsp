<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%-- recupera il token dalla sessione; il nome può variare a seconda della tua implementazione --%>
<%-- qui uso sessionScope.csrfToken come esempio (adattalo se il tuo fragment lo mette con un altro nome) --%>

<html><body>
<h1><c:out value="${plan != null ? 'Modifica scheda' : 'Nuova scheda'}"/></h1>

<form method="post"
      action="${pageContext.request.contextPath}/staff/plans/action?csrf=${sessionScope.csrfToken}"
      enctype="multipart/form-data">

  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>  <!-- => genera l'input hidden del token -->

  <input type="hidden" name="action" value="${plan != null ? 'update' : 'create'}"/>
  <c:if test="${plan != null}">
    <input type="hidden" name="id" value="${plan.id}"/>
  </c:if>

  Titolo: <input name="title" value="${plan.title}" required/><br/>
  Descrizione: <textarea name="description">${plan.description}</textarea><br/>
  Contenuto (json/text): <textarea name="content" rows="10" cols="80">${plan.content}</textarea><br/>

  <!-- UPLOAD -->
  <label>Allega scheda (PDF / Excel):</label>
  <input type="file" name="attachment" accept=".pdf,.xls,.xlsx,application/pdf,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" />
  ...
  <button type="submit">Salva</button>
</form>
</body></html>
