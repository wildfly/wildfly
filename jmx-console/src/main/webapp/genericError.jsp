<%-- A simple error page. --%>

<%@ page import="org.apache.catalina.util.ServerInfo, 
                 org.apache.catalina.valves.Constants, 
                 org.apache.catalina.util.StringManager" %>
<%@ page isErrorPage="true" %>

<html>
    <head>
        <style>
            <!--H1 {font-family:Tahoma, Arial, sans-serif; color:white; 
                    background-color:#525D76; font-size:22px;} 
                H3 {font-family:Tahoma, Arial, sans-serif; color:white; 
                    background-color:#525D76; font-size:14px;}  
                HR {color:#525D76;} 
                .errorText {font-family:Tahoma, Arial, sans-serif; font-size:16px; } -->
        </style>
        <title>
        <%
            StringManager sm = StringManager.getManager(Constants.Package);
            out.println(ServerInfo.getServerInfo() + " - " 
                        + sm.getString("errorReportValve.errorReport")); 
        %>
        </title>
    </head>
    <body>
        <h1>
        <% 
            out.println(sm.getString("errorReportValve.statusHeader",
                        "" + pageContext.getErrorData().getStatusCode(), "")); 
        %>
        </h1>
        <hr size=\"1\" noshade=\"noshade\">
        <span class="errorText">An error has occurred.</span>
        <hr size=\"1\" noshade=\"noshade\">
        <h3><%= ServerInfo.getServerInfo() %></h3>
    </body>
</html>

