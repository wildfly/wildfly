<html>
    <body>
        <%
    String id = request.getSession().getId();
    session.setAttribute("TEST_HTTP", id);
    // Expire after 10 secs so we can more promptly run timeout tests
    session.setMaxInactiveInterval(10);
        %>
        <p>Storing session id in attribute with id: <%=id%></p>
    </body>
</html>