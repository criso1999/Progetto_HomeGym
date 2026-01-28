<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<h1>Scegli un abbonamento</h1>

<c:if test="${not empty plans}">
  <form method="post" action="${pageContext.request.contextPath}/subscriptions">
    <input type="hidden" name="action" value="subscribe"/>
    <ul>
      <c:forEach var="p" items="${plans}">
        <li>
          <input type="radio" name="planId" value="${p.id}" id="plan${p.id}" ${p.id == plans[0].id ? 'checked' : ''}/>
          <label for="plan${p.id}">
            <strong>${p.name}</strong> - ${p.priceCents / 100.0} ${p.currency}
            <div>${p.description}</div>
          </label>
        </li>
      </c:forEach>
    </ul>

    <!-- Qui puoi inserire un metodo di pagamento inline o JS per aprire checkout provider -->
    <button type="submit">Procedi al pagamento</button>
  </form>
</c:if>
