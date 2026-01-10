<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="it.homegym.model.Utente" %>

<c:set var="client" value="${requestScope.client}" />
<%
  it.homegym.model.Utente cl = (it.homegym.model.Utente) request.getAttribute("client");
%>

<!doctype html>
<html>
<head><title><c:out value="${client != null ? 'Modifica cliente' : 'Nuovo cliente'}"/></title></head>
<body>
<h1><c:out value="${client != null ? 'Modifica cliente' : 'Nuovo cliente'}"/></h1>

<form method="post" action="${pageContext.request.contextPath}/staff/clients/action">
  <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
  <c:if test="${client != null}">
    <input type="hidden" name="action" value="update"/>
    <input type="hidden" name="id" value="${client.id}"/>
  </c:if>
  <c:if test="${client == null}">
    <input type="hidden" name="action" value="create"/>
  </c:if>

  Nome: <input name="nome" value="${client != null ? client.nome : ''}" required/><br/>
  Cognome: <input name="cognome" value="${client != null ? client.cognome : ''}" required/><br/>
  Email: <input name="email" type="email" value="${client != null ? client.email : ''}" required/><br/>
  <c:if test="${client == null}">
    Password (iniziale): <input name="password" type="password" required/><br/>
  </c:if>
  <c:if test="${client != null}">
    Nuova password (se vuoi cambiarla): <input name="password" type="password"/><br/>
  </c:if>

  <button type="submit">Salva</button>
</form>

<p><a href="${pageContext.request.contextPath}/staff/clients">← Torna alla lista</a></p>
</body>
</html>
