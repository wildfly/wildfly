<%@page isErrorPage="true"%>
<%
    response.setHeader("X-CustomErrorPage", "404.jsp");
%>
<h1>404 Error</h1>