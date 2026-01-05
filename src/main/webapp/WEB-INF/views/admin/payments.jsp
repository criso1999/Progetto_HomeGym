<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<!doctype html>
<html>
<head>
    <title>Admin - Payments</title>
    <style>
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; }
        th { background: #f4f4f4; }
        .pager { margin-top: 12px; }
        .pager a, .pager span { margin: 0 4px; padding:6px 10px; text-decoration:none; border:1px solid #ccc; }
        .pager .current { font-weight:bold; background:#eee; }
    </style>
</head>
<body>
<h1>Elenco pagamenti</h1>
<c:if test="${not empty sessionScope.flash}">
    <div class="notice" style="padding:8px;background:#e7f7e7;border:1px solid #bfe6bf;margin-bottom:12px;">
        <c:out value="${sessionScope.flash}" />
    </div>
    <c:remove var="flash" scope="session" />
</c:if>

<p>Totale pagamenti: <strong><c:out value="${totalCount}" /></strong></p>

<table>
    <thead>
        <tr>
            <th>ID</th>
            <th>User ID</th>
            <th>Amount</th>
            <th>Currency</th>
            <th>Status</th>
            <th>Created at</th>
        </tr>
    </thead>
    <tbody>
    <c:choose>
        <c:when test="${empty payments}">
            <tr><td colspan="6">Nessun pagamento</td></tr>
        </c:when>
        <c:otherwise>
            <c:forEach var="p" items="${payments}">
                <tr>
                    <td><c:out value="${p.id}" /></td>
                    <td>
                        <c:choose>
                            <c:when test="${p.userId != null}">
                                <c:out value="${p.userId}" />
                            </c:when>
                            <c:otherwise>-</c:otherwise>
                        </c:choose>
                    </td>
                    <td><fmt:formatNumber value="${p.amount}" minFractionDigits="2" maxFractionDigits="2"/></td>
                    <td><c:out value="${p.currency}" /></td>
                    <td><c:out value="${p.status}" /></td>
                    <td><c:out value="${p.createdAt}" /></td>

                    <!-- Colonna azioni -->
                    <td>
                        <!-- Mark PAID -->
                        <form method="post" action="${pageContext.request.contextPath}/admin/payments/action" style="display:inline;">
                            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                            <input type="hidden" name="action" value="updateStatus"/>
                            <input type="hidden" name="id" value="${p.id}"/>
                            <input type="hidden" name="status" value="PAID"/>
                            <input type="hidden" name="page" value="${page}" />
                            <input type="hidden" name="pageSize" value="${pageSize}" />
                            <button type="submit" <c:if test="${p.status == 'PAID'}">disabled</c:if> title="Segna come PAID">Mark PAID</button>
                        </form>

                        <!-- Mark REFUNDED -->
                        <form method="post" action="${pageContext.request.contextPath}/admin/payments/action" style="display:inline;">
                            <%@ include file="/WEB-INF/views/fragments/csrf.jspf" %>
                            <input type="hidden" name="action" value="updateStatus"/>
                            <input type="hidden" name="id" value="${p.id}"/>
                            <input type="hidden" name="status" value="REFUNDED"/>
                            <input type="hidden" name="page" value="${page}" />
                            <input type="hidden" name="pageSize" value="${pageSize}" />
                            <button type="submit" <c:if test="${p.status == 'REFUNDED'}">disabled</c:if> title="Segna come REFUNDED">Mark REFUNDED</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
        </c:otherwise>
    </c:choose>
    </tbody>
</table>

<div class="pager">
    <c:if test="${page > 1}">
        <a href="${pageContext.request.contextPath}/admin/payments?page=${page - 1}&pageSize=${pageSize}">Prev</a>
    </c:if>

    <c:forEach begin="1" end="${totalPages}" var="i">
        <c:choose>
            <c:when test="${i == page}">
                <span class="current">${i}</span>
            </c:when>
            <c:otherwise>
                <a href="${pageContext.request.contextPath}/admin/payments?page=${i}&pageSize=${pageSize}">${i}</a>
            </c:otherwise>
        </c:choose>
    </c:forEach>

    <c:if test="${page < totalPages}">
        <a href="${pageContext.request.contextPath}/admin/payments?page=${page + 1}&pageSize=${pageSize}">Next</a>
    </c:if>
</div>

<form method="get" action="${pageContext.request.contextPath}/admin/payments" style="margin-top:10px;">
    <label>Pagina: <input type="number" name="page" min="1" max="${totalPages}" value="${page}" style="width:60px;" /></label>
    <label>Per pagina:
        <select name="pageSize">
            <option value="5" <c:if test="${pageSize == 5}">selected</c:if>>5</option>
            <option value="10" <c:if test="${pageSize == 10}">selected</c:if>>10</option>
            <option value="25" <c:if test="${pageSize == 25}">selected</c:if>>25</option>
            <option value="50" <c:if test="${pageSize == 50}">selected</c:if>>50</option>
        </select>
    </label>
    <button type="submit">Vai</button>
</form>

<p><a href="${pageContext.request.contextPath}/admin/home">← Admin Home</a></p>
</body>
</html>
