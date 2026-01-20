<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html><body>
<h1>Storico versione scheda</h1>
<c:forEach var="v" items="${history}">
  <div style="border:1px solid #ccc; padding:8px; margin:8px 0">
    <strong>Versione ${v.versionNumber}</strong> - <small>${v.createdAt}</small><br/>
    <strong>${v.title}</strong>
    <pre style="white-space:pre-wrap">${v.content}</pre>
  </div>
</c:forEach>
<p><a href="${pageContext.request.contextPath}/staff/plans">← Torna</a></p>
</body></html>
