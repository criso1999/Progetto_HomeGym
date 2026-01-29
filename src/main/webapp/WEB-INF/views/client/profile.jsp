<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Il mio profilo</title>
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

<c:if test="${not empty info}">
  <div class="flash" style="color:green">${info}</div>
</c:if>
<c:if test="${not empty error}">
  <div class="flash" style="color:red">${error}</div>
</c:if>

<c:set var="u" value="${user != null ? user : sessionScope.user}" />

<div class="card">
  <h2>Dati anagrafici</h2>
  <p><strong>Nome:</strong> <c:out value="${u.nome}"/></p>
  <p><strong>Cognome:</strong> <c:out value="${u.cognome}"/></p>
  <p><strong>Email:</strong> <c:out value="${u.email}"/></p>
  <p class="muted">Modifica i campi qui sotto e clicca "Salva" per aggiornare le informazioni del profilo.</p>
</div>

<div class="card">
  <h2>Aggiorna profilo</h2>

  <form method="post" action="${pageContext.request.contextPath}/client/profile">
    <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
    <input type="hidden" name="action" value="update"/>

    <div class="row">
      <div class="col">
        <label for="nome">Nome</label>
        <input id="nome" name="nome" type="text" value="${fn:escapeXml(u.nome)}" />
      </div>
      <div class="col">
        <label for="cognome">Cognome</label>
        <input id="cognome" name="cognome" type="text" value="${fn:escapeXml(u.cognome)}" />
      </div>
    </div>

    <label for="email">Email</label>
    <input id="email" name="email" type="email" value="${fn:escapeXml(u.email)}" />

    <label for="telefono">Telefono (opzionale)</label>
    <input id="telefono" name="telefono" type="text" value="${fn:escapeXml(u.telefono)}" />

    <label for="bio">Note / Bio (opzionale)</label>
    <textarea id="bio" name="bio" rows="4">${fn:escapeXml(u.bio)}</textarea>

    <h3>Modifica password (opzionale)</h3>
    <label for="password">Nuova password</label>
    <input id="password" name="password" type="password" />
    <label for="password2">Ripeti nuova password</label>
    <input id="password2" name="password2" type="password" />

    <div class="actions">
      <button type="submit">Salva</button>
      &nbsp;
      <a href="${pageContext.request.contextPath}/client/home">Annulla / Torna</a>
    </div>
  </form>
</div>

<div class="card">
  <h2>Il mio Feed</h2>
  <p><a href="${pageContext.request.contextPath}/posts">Vai al mio feed</a></p>
</div>

</body>
</html>
