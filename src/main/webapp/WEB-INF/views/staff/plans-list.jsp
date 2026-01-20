<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html><body>
<h1>Le tue schede assegnate</h1>
<c:if test="${empty assignments}">Non hai schede assegnate.</c:if>
<c:forEach var="a" items="${assignments}">
  <div style="border:1px solid #ddd;padding:8px;margin:6px 0">
    <p>Assegnata: <c:out value="${a.assignedAt}"/></p>
    <p>Scheda id: <c:out value="${a.planId}"/> — <a href="${pageContext.request.contextPath}/posts">Apri scheda</a></p>
    <p>Note: <c:out value="${a.notes}"/></p>
    <p>Stato: <c:out value="${a.active ? 'Attiva' : 'Non attiva'}"/></p>
  </div>
</c:forEach>
</body></html>
