<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="it.homegym.model.Utente" %>
<%
    Utente u = (Utente) session.getAttribute("user");
    if (u == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Il mio abbonamento</title>
  <style>
    body { font-family: Arial, sans-serif; }
    .box { border:1px solid #eee; padding:12px; margin:12px 0; background:#fafafa; }
    .muted { color:#666; font-size:0.9em; }
    .actions form { display:inline; margin-right:8px; }
    .flash { margin-bottom:10px; }
  </style>
</head>
<body>

<h1>Il mio abbonamento</h1>

<c:if test="${not empty sessionScope.flashSuccess}">
  <div class="flash" style="color:green">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div class="flash" style="color:red">${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>

<c:choose>
  <c:when test="${not empty subscription}">
    <div class="box">
      <p><strong>Utente:</strong> <c:out value="${sessionScope.user.nome}"/> <c:out value="${sessionScope.user.cognome}"/></p>

      <p><strong>Stato:</strong> <c:out value="${subscription.status}"/></p>
      <p><strong>Piano (ID):</strong> <c:out value="${subscription.planId}"/></p>
      <p><strong>Prezzo:</strong> <c:out value="${subscription.priceCents}"/> <span class="muted">${subscription.currency}</span></p>
      <p><strong>Inizio:</strong> <c:out value="${subscription.startDate}"/></p>
      <p><strong>Fine:</strong> <c:out value="${subscription.endDate}"/></p>
      <p><strong>Provider pagamento:</strong> <c:out value="${subscription.paymentProvider}"/></p>

      <div class="actions">
        <a href="${pageContext.request.contextPath}/subscriptions">Scegli / Cambia piano</a>

        <c:if test="${subscription.status == 'PENDING' || subscription.status == 'ACTIVE'}">
          <form method="post" action="${pageContext.request.contextPath}/account/subscription" style="display:inline">
            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
            <input type="hidden" name="action" value="cancel"/>
            <input type="hidden" name="subscriptionId" value="${subscription.id}"/>
            <button type="submit" onclick="return confirm('Confermi cancellazione dell\'abbonamento?')">Cancella abbonamento</button>
          </form>
        </c:if>

      </div>
    </div>

    <p><a href="${pageContext.request.contextPath}/client/home">← Torna alla home</a></p>

  </c:when>

  <c:otherwise>
    <div class="box">
      <p>Non hai abbonamenti attivi al momento.</p>
      <p>
        <a href="${pageContext.request.contextPath}/subscriptions">Sottoscrivi un abbonamento</a>
      </p>
    </div>
    <p><a href="${pageContext.request.contextPath}/client/home">← Torna alla home</a></p>
  </c:otherwise>
</c:choose>

</body>
</html>