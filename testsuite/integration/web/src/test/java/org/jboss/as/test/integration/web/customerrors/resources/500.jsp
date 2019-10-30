<%@ page isErrorPage="true"%>
<%
    Throwable error = (Throwable) request.getAttribute("javax.servlet.error.exception");
    response.setHeader("X-CustomErrorPage", "500.jsp");
    if (error != null)
        response.setHeader("X-ExceptionType", error.getClass().getName());
%>
<h1>500 Error</h1>
