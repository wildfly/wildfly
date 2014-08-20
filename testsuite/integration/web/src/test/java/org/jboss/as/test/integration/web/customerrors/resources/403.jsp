<%@page isErrorPage="true"%>
<%
    response.setHeader("X-CustomErrorPage", "403.jsp");
%>
<h1>403 Error</h1>