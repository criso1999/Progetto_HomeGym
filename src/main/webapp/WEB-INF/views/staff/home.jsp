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
    <title>Staff - Home</title>
    <style>
        .section {
            border: 1px solid #ddd;
            padding: 12px;
            margin: 16px 0;
        }
        ul { padding-left: 18px; }
    </style>
</head>
<body>

<h1>Staff Dashboard</h1>

<p>
    Benvenuto
    <strong><%= u.getNome() %> <%= u.getCognome() %></strong>
    (<%= u.getRuolo() %>)
</p>

<!-- ===== MANAGEMENT ===== -->
<div class="section">
    <h3>Gestione</h3>
    <ul>
        <li><a href="<%=request.getContextPath()%>/staff/clients">Clienti</a></li>
        <li><a href="<%=request.getContextPath()%>/staff/sessions">Sessioni</a></li>
        <li><a href="<%=request.getContextPath()%>/staff/plans/form">Schede Allenamento</a></li>
    </ul>
</div>

<!-- ===== COMMUNITY FEED ===== -->
<div class="section">
    <h3>Community Feed</h3>
    <p>
        Visualizza i post e gli allenamenti condivisi dai tuoi clienti,
        commenta e lascia una valutazione.
    </p>
    <p>
        <a href="<%=request.getContextPath()%>/staff/community">
            👉 Vai al Community Feed
        </a>
    </p>
</div>

<!-- ===== ACCOUNT ===== -->
<div class="section">
    <h3>Account</h3>
    <p><a href="<%=request.getContextPath()%>/logout">Logout</a></p>
</div>

</body>
</html>
