<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="it.homegym.model.TrainingSession" %>
<%@ page import="it.homegym.model.Presence" %>

<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>QR Check-in / Presenze</title>
  <style>
    .small { font-size:0.9em; color:#666; }
    .qr-box { display:flex; gap:20px; align-items:center; margin-top:10px; }
    .qr-img { border:1px solid #ddd; padding:8px; background:#fff; }
    table { border-collapse:collapse; margin-top:18px; width:100%; }
    th,td { border:1px solid #ddd; padding:6px; text-align:left; }
  </style>
</head>
<body>

<c:if test="${not empty sessionScope.flashSuccess}">
  <div style="color:green">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div style="color:red">${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>

<h1>QR Check-in per la sessione</h1>

<c:choose>
  <c:when test="${empty sessionScope.user}">
    <p>Devi essere autenticato.</p>
  </c:when>
  <c:otherwise>
    <c:set var="ctx" value="${pageContext.request.contextPath}" />

    <!-- Informazioni sulla sessione (se fornite dal servlet) -->
    <c:if test="${not empty session}">
      <p>
        <strong>Sessione:</strong>
        <c:out value="${session.id}"/> —
        <c:out value="${session.trainer}"/> —
        <span class="small">
          <fmt:formatDate value="${session.when}" pattern="yyyy-MM-dd HH:mm"/>
        </span>
      </p>
    </c:if>

    <!-- Controllo ruolo: solo PERSONALE / PROPRIETARIO possono generare il QR -->
    <c:if test="${sessionScope.user.ruolo == 'PERSONALE' || sessionScope.user.ruolo == 'PROPRIETARIO'}">

      <div>
        <label class="small">Scadenza QR (minuti):</label>
        <input id="expiresInput" type="number" min="1" value="30" style="width:80px" />
        <button id="genBtn" type="button">Genera / Rigenera QR</button>
        &nbsp;
        <a id="downloadLink" href="#" download="qr-checkin.png" style="display:none">⬇ Scarica PNG</a>
      </div>

      <div class="qr-box">
        <div>
          <div class="small">QR (scansiona per registrare la presenza)</div>
          <img id="qrImage" class="qr-img" src="" alt="QR check-in" width="300" height="300"/>
        </div>

        <div>
          <p class="small">Suggerimenti:</p>
          <ul class="small">
            <li>QR valido per il tempo impostato; è monouso (se previsto dal sistema).</li>
            <li>Usa HTTPS in produzione per evitare intercettazioni del token.</li>
            <li>Se vuoi evitare caching aggiungi parametro `&_ts=` (la pagina lo fa automaticamente).</li>
          </ul>
        </div>
      </div>

      <!-- Opzionale: lista presenze per la sessione -->
      <c:if test="${not empty presences}">
        <h3>Presenze registrate</h3>
        <table>
          <thead><tr><th>ID</th><th>Utente</th><th>Check-in</th><th>Scansionato da (id)</th><th>Token</th><th>Used</th></tr></thead>
          <tbody>
            <c:forEach var="p" items="${presences}">
              <tr>
                <td><c:out value="${p.id}"/></td>
                <td><c:out value="${p.userId != null ? p.userId : '-'}"/></td>
                <td><c:out value="${p.checkinAt != null ? p.checkinAt : '-'}"/></td>
                <td><c:out value="${p.scannedBy != null ? p.scannedBy : '-'}"/></td>
                <td><c:out value="${p.token}"/></td>
                <td><c:out value="${p.used ? 'Sì' : 'No'}"/></td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </c:if>

    </c:if>

    <!-- Se non autorizzato -->
    <c:if test="${not (sessionScope.user.ruolo == 'PERSONALE' || sessionScope.user.ruolo == 'PROPRIETARIO')}">
      <p class="small">Non hai i permessi per generare QR per il check-in.</p>
    </c:if>

  </c:otherwise>
</c:choose>

<script>
  (function(){
    const ctx = '${pageContext.request.contextPath}';
    const sessionId = '${session != null ? session.id : ""}';
    const img = document.getElementById('qrImage');
    const dl = document.getElementById('downloadLink');
    const genBtn = document.getElementById('genBtn');

    function buildUrl(expires) {
      const base = ctx + '/staff/sessions/qr';
      const params = new URLSearchParams();
      if (sessionId) params.set('sessionId', sessionId);
      if (expires) params.set('expires', expires);
      // timestamp per busting cache
      params.set('_ts', Date.now());
      return base + '?' + params.toString();
    }

    function regen() {
      const expires = document.getElementById('expiresInput').value || '30';
      const url = buildUrl(expires);
      img.src = url;
      // mostra link per scaricare lo stesso url (attiva download)
      dl.href = url;
      dl.style.display = 'inline';
    }

    // rigenera al click
    if (genBtn) genBtn.addEventListener('click', regen);

    // alla prima apertura, mostra QR con default
    window.addEventListener('load', function(){
      if (genBtn) regen();
    });
  })();
</script>

</body>
</html>
