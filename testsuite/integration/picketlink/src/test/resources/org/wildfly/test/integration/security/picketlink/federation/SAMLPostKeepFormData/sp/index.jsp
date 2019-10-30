<%
  String testData = request.getParameter("test-data");
%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Post Test</title>
</head>
<body>
  <h1>Post Test</h1>
    <table>
      <tr>
        <td>Test Data:</td>
        <td><%=testData%></td>
      </tr>
    </table>
  </body>  
</html>