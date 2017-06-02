<?xml version="1.0"?>
<%@page contentType="text/html"
   import="java.net.*,
           java.io.*,
   		   java.beans.PropertyEditor,
   		   org.jboss.util.propertyeditor.PropertyEditors"
%>
<%
String hostname = "";
try
{
  hostname = InetAddress.getLocalHost().getHostName();
}
catch(IOException e){}
%>

<!DOCTYPE html 
    PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
<head>
   <title>Operation Results</title>
   <link rel="stylesheet" href="style_master.css" type="text/css" />
   <meta http-equiv="cache-control" content="no-cache" />
</head>

<jsp:useBean id='opResultInfo' class='org.jboss.jmx.adaptor.control.OpResultInfo' type='org.jboss.jmx.adaptor.control.OpResultInfo' scope='request'/>
<%
   if(opResultInfo.name == null)
   {
%>
  	<jsp:forward page="/" />

<%
   }
%>    
<body leftmargin="10" rightmargin="10" topmargin="10">

<table width="100%" cellspacing="0" cellpadding="0" border="0" align="center">
 <tr>
  <td height="105" align="center"><h1>JMX MBean Operation View</h1><%= hostname %></td>
  <td height="105" align="center" width="300">
    <p>
      <input type="button" value="Back to Agent" onClick="javascript:location='HtmlAdaptor?action=displayMBeans'"/>
      <input type="button" value="Back to MBean" onClick="javascript:location='HtmlAdaptor?action=inspectMBean&amp;name=<%= request.getParameter("name") %>'"/>
    </p>
    <p>
    <%
      out.print("<input type='button' onClick=\"location='HtmlAdaptor?action=invokeOpByName");
      out.print("&amp;name=" + request.getParameter("name"));
      out.print("&amp;methodName=" + opResultInfo.name );
    
      for (int i=0; i<opResultInfo.args.length; i++)
      {
        out.print("&amp;argType=" + opResultInfo.signature[i]);
        out.print("&amp;arg" + i + "=" + opResultInfo.args[i]);
      }
    
      out.println("'\" value='Reinvoke MBean Operation'/>");
    %>
    </p>
  </td>
 </tr>
</table>

<%
   if( opResultInfo.result == null )
   {
     out.println("Operation completed successfully without a return value!");
   }
   else
   {
      String opResultString = null;

      PropertyEditor propertyEditor = PropertyEditors.findEditor(opResultInfo.result.getClass());
      if(propertyEditor != null)
      {
         propertyEditor.setValue(opResultInfo.result);
         opResultString = propertyEditor.getAsText();
      }
      else
      {
         opResultString = opResultInfo.result.toString();
      }

      boolean hasPreTag = opResultString.startsWith("<pre>");
      if( hasPreTag == false ) out.println("<pre>");
      out.println(opResultString);
      if( hasPreTag == false ) out.println("</pre>");
   }
%>
</body>
</html>
