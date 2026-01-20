<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title><c:out value="${sessionScope.session != null ? 'Modifica' : 'Nuova'} sessione"/></title>
  <style>
    label { display:block; margin:6px 0; }
    .error { color: red; }
  </style>
</head>
<body>
  <h1><c:out value="${sessionScope.session != null ? 'Modifica' : 'Nuova'} sessione"/></h1>

  <c:if test="${not empty error}">
    <div class="error">${error}</div>
  </c:if>

  <c:if test="${sessionScope.user.ruolo == 'PERSONALE' || sessionScope.user.ruolo == 'PROPRIETARIO'}">
    <p>Genera QR per il check-in (scadenza 30 min):</p>
    <img src="${pageContext.request.contextPath}/staff/sessions/qr?sessionId=${session.id}&expires=30" alt="QR check-in" />
    <p class="small">Scansiona il QR per registrare la presenza.</p>
  </c:if>


  <form id="sessionForm" method="post" action="${pageContext.request.contextPath}/staff/sessions/action">
    <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>

    <input type="hidden" name="action" value="${sessionScope.session != null ? 'update' : 'create'}"/>
    <c:if test="${sessionScope.session != null}">
      <input type="hidden" name="id" value="${sessionScope.session.id}" />
    </c:if>

    <label>
      Utente (opzionale):
      <input name="userId" type="number" min="1" value="${sessionScope.session != null ? sessionScope.session.userId : ''}" />
    </label>

    <label>
      Trainer:
      <select name="trainer" required>
        <option value="">-- seleziona trainer --</option>
        <c:forEach var="t" items="${requestScope.trainers}">
          <option value="${t.nome} ${t.cognome}"
            <c:if test="${sessionScope.session != null && (sessionScope.session.trainer == (t.nome + ' ' + t.cognome))}">selected</c:if>>
            ${fn:escapeXml(t.nome)} ${fn:escapeXml(t.cognome)} (${fn:escapeXml(t.email)})
          </option>
        </c:forEach>
      </select>
    </label>

    <label>
      Data / Ora:
      <input id="scheduledAt" type="datetime-local" name="scheduled_at"
             value="${fn:escapeXml(requestScope.scheduledAtInput)}" required />
    </label>

    <label>
      Durata (min):
      <input name="duration" type="number" min="1" value="${sessionScope.session != null ? sessionScope.session.durationMinutes : '60'}" required />
    </label>

    <label>
      Note:<br/>
      <textarea name="notes" rows="4" cols="60">${sessionScope.session != null ? fn:escapeXml(sessionScope.session.notes) : ''}</textarea>
    </label>

    <button type="submit">${sessionScope.session != null ? 'Aggiorna' : 'Crea'}</button>
  </form>

  <script>
    // semplice validazione client-side
    document.getElementById('sessionForm').addEventListener('submit', function(e){
      var trainer = this.querySelector('select[name="trainer"]').value;
      var sched = this.querySelector('input[name="scheduled_at"]').value;
      if (!trainer) {
        alert('Seleziona un trainer.');
        e.preventDefault();
        return false;
      }
      if (!sched) {
        alert('Inserisci data e ora della sessione.');
        e.preventDefault();
        return false;
      }
      return true;
    });
  </script>

  <p><a href="${pageContext.request.contextPath}/staff/sessions">← Torna all'elenco sessioni</a></p>
</body>
</html>
