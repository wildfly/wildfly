<%@page isErrorPage="true" contentType="text/html" %>
<html>
    <title>404 - Page not found</title>
    <body background="collage_1920x1200.jpg" text="#FFFFFF">
        <center>
        <h2>404 - Page not found.</h2>
        <p>
        <br/>
        Request that failed: ${pageContext.errorData.requestURI}
        <br/>
        Status code: ${pageContext.errorData.statusCode}
        <br/>
        Exception: ${pageContext.errorData.throwable}
        <br/>
        ${pageContext.errorData.servletName}
        </p>
        </center>
    </body>
</html>
