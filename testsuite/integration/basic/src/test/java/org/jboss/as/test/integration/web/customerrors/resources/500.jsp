<%@ page import="java.util.Enumeration"%>
<%@ page isErrorPage="true"%>
<%
    Throwable error = (Throwable) request.getAttribute("javax.servlet.error.exception");
    Enumeration<String> iter = request.getAttributeNames();
    while (iter.hasMoreElements()) {
        String name = (String) iter.nextElement();
        Object value = request.getAttribute(name);
        System.out.println(name + " = " + value);
    }
    response.setHeader("X-CustomErrorPage", "500.jsp");
    if (error != null)
        response.setHeader("X-ExceptionType", error.getClass().getName());
%>
<h1>500 Error</h1>
