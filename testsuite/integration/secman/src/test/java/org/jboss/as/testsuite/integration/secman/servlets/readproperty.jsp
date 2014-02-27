<%@ page language="java" pageEncoding="UTF-8" contentType="text/plain; charset=UTF-8" session="false"
%><%!
private String defaultString(String str, String defVal) {
    return (str != null && str.length() > 0)?str:defVal;
}
%>${(empty param.property)?"java.home":param.property}=<%= System.getProperty(defaultString(request.getParameter("property"),"java.home")) %>
