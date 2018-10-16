<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <title>Security Manager</title>
        <link rel="stylesheet" href="style.css" type="text/css" />
    </head>
    <body>
        <h1>test</h1>
        <p>Security manager used: <%= request.getSession().getId() %></p>
    </body>
</html>
