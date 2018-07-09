<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>error</title>
</head>
<body>

  error.jsp

  <% // =exception %>

  <%
  out.println("message="+request.getAttribute("javax.servlet.error.message"));
  %>

</body>
</html>
