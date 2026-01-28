<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Gestione Abbonamenti - Staff</title>
  <style>
    body { font-family: Arial, sans-serif; }
    .flash-success { color: green; margin-bottom: 8px; }
    .flash-error { color: red; margin-bottom: 8px; }
    table { border-collapse:collapse; width:100%; margin: 8px 0 20px; }
    th,td { border:1px solid #ddd; padding:8px; vertical-align: middle; }
    th { background:#f5f5f5; text-align:left; }
    .small { color:#666; font-size:0.9em }
    form.inline { display:inline; margin:0; }
    input[type="text"], input[type="number"], select { padding:4px; }
    .box { border:1px solid #eee; padding:12px; margin:12px 0; background:#fafafa; }
    .actions button { margin-left:6px; }
  </style>
</head>
<body>

<h1>Gestione Abbonamenti (Staff)</h1>

<c:if test="${not empty sessionScope.flashSuccess}">
  <div class="flash-success">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div class="flash-error">${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>

<!-- FORM: crea nuovo piano -->
<div class="box">
  <h3>Nuovo piano</h3>
  <form method="post" action="${pageContext.request.contextPath}/staff/subscriptions">
    <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
    <input type="hidden" name="action" value="create-plan"/>
    <label>Nome: <input type="text" name="name" required/></label>
    &nbsp;
    <label>Periodo:
      <select name="period">
        <option value="MONTHLY">Mensile</option>
        <option value="SEMESTRAL">Semestrale</option>
        <option value="ANNUAL">Annuale</option>
        <option value="90">90 giorni</option>
      </select>
    </label>
    &nbsp;
    <label>Prezzo (cents): <input type="number" name="priceCents" required min="0"/></label>
    &nbsp;
    <label>Valuta: <input type="text" name="currency" value="EUR" size="4"/></label>
    &nbsp;
    <label><input type="checkbox" name="active" value="1" checked/> Attivo</label>
    <br/><br/>
    <label>Descrizione: <input type="text" name="description" size="80"/></label>
    &nbsp;
    <button type="submit">Crea piano</button>
  </form>
</div>

<!-- TABELLA: Piani -->
<h3>Piani disponibili</h3>
<table>
  <thead>
    <tr>
      <th>ID</th><th>Code</th><th>Nome</th><th>Durata (giorni)</th><th>Prezzo (cents)</th><th>Valuta</th><th>Attivo</th><th>Azioni</th>
    </tr>
  </thead>
  <tbody>
    <c:choose>
      <c:when test="${empty plans}">
        <tr><td colspan="8">Nessun piano trovato.</td></tr>
      </c:when>
      <c:otherwise>
        <c:forEach var="p" items="${plans}">
          <tr>
            <td><c:out value="${p.id}"/></td>
            <td><c:out value="${p.code}"/></td>
            <td><c:out value="${p.name}"/></td>
            <td class="small"><c:out value="${p.durationDays}"/></td>
            <td><c:out value="${p.priceCents}"/></td>
            <td><c:out value="${p.currency}"/></td>
            <td><c:out value="${p.active ? 'Sì' : 'No'}"/></td>
            <td class="actions">
              <!-- update inline -->
              <form class="inline" method="post" action="${pageContext.request.contextPath}/staff/subscriptions">
                <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                <input type="hidden" name="action" value="update-plan"/>
                <input type="hidden" name="planId" value="${p.id}"/>
                <input type="hidden" name="period" value="${p.code}"/>
                <input type="hidden" name="priceCents" value="${p.priceCents}"/>
                <button type="submit">Modifica (apri form)</button>
              </form>

              <!-- delete -->
              <form class="inline" method="post" action="${pageContext.request.contextPath}/staff/subscriptions" onsubmit="return confirm('Confermi rimozione del piano?');">
                <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                <input type="hidden" name="action" value="delete-plan"/>
                <input type="hidden" name="planId" value="${p.id}"/>
                <button type="submit">Elimina</button>
              </form>
            </td>
          </tr>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </tbody>
</table>

<!-- TABELLA: Sottoscrizioni -->
<h3>Sottoscrizioni utenti</h3>
<table>
  <thead>
    <tr>
      <th>ID</th><th>User ID</th><th>Plan ID</th><th>Stato</th><th>Start</th><th>End</th><th>Prezzo</th><th>Provider</th><th>Azioni</th>
    </tr>
  </thead>
  <tbody>
    <c:choose>
      <c:when test="${empty subscriptions}">
        <tr><td colspan="9">Nessuna sottoscrizione trovata.</td></tr>
      </c:when>
      <c:otherwise>
        <c:forEach var="s" items="${subscriptions}">
          <tr>
            <td><c:out value="${s.id}"/></td>
            <td><a href="${pageContext.request.contextPath}/staff/clients?userId=${s.userId}"><c:out value="${s.userId}"/></a></td>
            <td><c:out value="${s.planId}"/></td>
            <td><c:out value="${s.status}"/></td>
            <td><c:out value="${s.startDate}"/></td>
            <td><c:out value="${s.endDate}"/></td>
            <td><c:out value="${s.priceCents}"/> <span class="small">${s.currency}</span></td>
            <td><c:out value="${s.paymentProvider}"/></td>
            <td>
              <c:choose>
                <c:when test="${s.status == 'PENDING'}">
                  <form class="inline" method="post" action="${pageContext.request.contextPath}/staff/subscriptions">
                    <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                    <input type="hidden" name="action" value="activate"/>
                    <input type="hidden" name="subscriptionId" value="${s.id}"/>
                    <button type="submit">Attiva</button>
                  </form>
                  <form class="inline" method="post" action="${pageContext.request.contextPath}/staff/subscriptions">
                    <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                    <input type="hidden" name="action" value="cancel"/>
                    <input type="hidden" name="subscriptionId" value="${s.id}"/>
                    <button type="submit" onclick="return confirm('Annullare sottoscrizione PENDING?');">Annulla</button>
                  </form>
                </c:when>
                <c:when test="${s.status == 'ACTIVE'}">
                  <form class="inline" method="post" action="${pageContext.request.contextPath}/staff/subscriptions" onsubmit="return confirm('Confermi cancellazione/terminazione?');">
                    <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                    <input type="hidden" name="action" value="cancel"/>
                    <input type="hidden" name="subscriptionId" value="${s.id}"/>
                    <button type="submit">Cancella</button>
                  </form>
                </c:when>
                <c:otherwise>
                  <span class="small">N/A</span>
                </c:otherwise>
              </c:choose>
            </td>
          </tr>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </tbody>
</table>

<p><a href="${pageContext.request.contextPath}/staff/home">← Torna</a></p>

</body>
</html>
