<%@ page import="java.util.Enumeration,
                 java.io.StringWriter,
                 java.io.PrintWriter"%>
<%@ page isErrorPage="true" %>

<html>
<head>
<title>Error Page</title>
</head>

<h1>Request Attributes</h1>
<ul>
<%
   Enumeration<String> names = request.getAttributeNames();
   while( names.hasMoreElements() )
   {
      String name = (String) names.nextElement();
      Object value = request.getAttribute(name);
      out.println("<li>Name:<b>"+name+"</b> = "+value+"</li>");
   }
%>
</ul>
<h1>Session Attributes</h1>
<ul>
<%
   names = session.getAttributeNames();
   while( names.hasMoreElements() )
   {
      String name = (String) names.nextElement();
      Object value = session.getAttribute(name);
      out.println("<li>Name:<b>"+name+"</b> = "+value+"</li>");
   }
   // Add an X-JException header if we see the j_exception attribute
   Object j_exception = session.getAttribute("j_exception");
   if( j_exception != null )
   {
      response.setHeader("X-JException", j_exception.getClass().getName());
   }
   else
   {
      response.setHeader("X-NoJException", "true");
      //Thread.dumpStack();
   }
%>
</ul>
<h1>Caller Trace</h1>
<pre>
<%
   Throwable t = new Throwable();
   PrintWriter pw = new PrintWriter(out);
   t.printStackTrace(pw);
   pw.flush();
%>
</pre>
</html>
