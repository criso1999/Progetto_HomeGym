<c:if test="${not empty sessionScope.flashSuccess}">
  <div style="color:green">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div style="color:red">${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>

<c:if test="${not empty info}"><div style="color:green">${info}</div></c:if>
<c:if test="${not empty error}"><div style="color:red">${error}</div></c:if>
