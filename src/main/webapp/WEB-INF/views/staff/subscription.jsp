<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<h1>Il tuo abbonamento</h1>

<c:if test="${empty subscription}">
  <p>Non hai un abbonamento attivo. <a href="${pageContext.request.contextPath}/subscriptions">Sottoscrivine uno</a></p>
</c:if>

<c:if test="${not empty subscription}">
  <p>Stato: <strong>${subscription.status}</strong></p>
  <p>Inizio: <strong>${subscription.startDate}</strong></p>
  <p>Fine: <strong>${subscription.endDate}</strong></p>
  <form method="post" action="${pageContext.request.contextPath}/account/subscription/cancel">
    <button type="submit">Cancella abbonamento</button>
  </form>
</c:if>
