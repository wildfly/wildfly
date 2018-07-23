<%
  String testData = request.getParameter("test-data");
  String[] testDataValues = request.getParameterValues("test-data");
  java.util.Map parameterMap = request.getParameterMap();
  java.util.Enumeration parameterNames = request.getParameterNames();
  int parameterNamesCount = 0;
  while (parameterNames.hasMoreElements()) {
    parameterNames.nextElement();
    parameterNamesCount++;
  }
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
        <td>Test Data: <%=testData%></td>
        <td>Test Data Values: <%=testDataValues==null?"null":testDataValues.length%></td>
        <td>Parameters Map Size: <%=parameterMap==null?"null":parameterMap.size()%></td>
        <td>Parameters Names Size: <%=parameterNamesCount%></td>
      </tr>
    </table>
  </body>  
</html>