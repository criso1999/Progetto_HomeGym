<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="it.homegym.model.Utente" %>
<%
    Utente u = (Utente) session.getAttribute("user");
    if (u == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
    if (!"PERSONALE".equals(u.getRuolo()) && !"PROPRIETARIO".equals(u.getRuolo())) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return;
    }
%>
<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Profilo Staff</title>
  <style>
    body { font-family: Arial, sans-serif; max-width:900px; margin:20px auto; }
    .card { border:1px solid #ddd; padding:16px; margin-bottom:16px; border-radius:6px; background:#fff; }
    label { display:block; margin-top:8px; font-weight:600; }
    input[type="text"], input[type="email"], textarea { width:100%; padding:8px; box-sizing:border-box; margin-top:4px; }
    .row { display:flex; gap:12px; }
    .col { flex:1; }
    .actions { margin-top:12px; }
    .muted { color:#666; font-size:0.9em; }
    .flash { margin-bottom:12px; }
  </style>
</head>
<body>

<h1>Il mio profilo</h1>

<c:if test="${not empty sessionScope.flashSuccess}">
  <div class="flash" style="color:green">${sessionScope.flashSuccess}</div>
  <c:remove var="flashSuccess" scope="session"/>
</c:if>
<c:if test="${not empty sessionScope.flashError}">
  <div class="flash" style="color:red">${sessionScope.flashError}</div>
  <c:remove var="flashError" scope="session"/>
</c:if>

<div class="card">
  <h2>Dati anagrafici</h2>
  <p><strong>Nome:</strong> <%= u.getNome() != null ? u.getNome() : "" %></p>
  <p><strong>Cognome:</strong> <%= u.getCognome() != null ? u.getCognome() : "" %></p>
  <p><strong>Email:</strong> <%= u.getEmail() != null ? u.getEmail() : "" %></p>
  <p><strong>Ruolo:</strong> <%= u.getRuolo() != null ? u.getRuolo() : "" %></p>
  <p class="muted">Modifica i campi qui sotto e clicca "Salva" per aggiornare le informazioni del profilo.</p>
</div>

<div class="card">
  <h2>Aggiorna profilo</h2>

  <form method="post" action="${pageContext.request.contextPath}/staff/profile">
    <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
    <input type="hidden" name="action" value="update"/>

    <div class="row">
      <div class="col">
        <label for="nome">Nome</label>
        <input id="nome" name="nome" type="text" value="<%= u.getNome() != null ? u.getNome() : "" %>" />
      </div>
      <div class="col">
        <label for="cognome">Cognome</label>
        <input id="cognome" name="cognome" type="text" value="<%= u.getCognome() != null ? u.getCognome() : "" %>" />
      </div>
    </div>

    <label for="email">Email</label>
    <input id="email" name="email" type="email" value="<%= u.getEmail() != null ? u.getEmail() : "" %>" />

    <label for="telefono">Telefono (opzionale)</label>
    <input id="telefono" name="telefono" type="text" value="<%= u.getTelefono() != null ? u.getTelefono() : "" %>" />

    <label for="bio">Note / Bio (opzionale)</label>
    <textarea id="bio" name="bio" rows="4"><%= u.getBio() != null ? u.getBio() : "" %></textarea>

    <div class="actions">
      <button type="submit">Salva</button>
      &nbsp;
      <a href="${pageContext.request.contextPath}/staff/home">Annulla / Torna alla dashboard</a>
    </div>
  </form>
</div>

<div class="card">
  <h2>Sicurezza</h2>
  <p>Per cambiare la password: <a href="${pageContext.request.contextPath}/account/change-password">Cambia password</a></p>
</div>

</body>
</html>
