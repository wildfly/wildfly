<?xml version="1.0"?>
<%@page contentType="text/html" import="java.net.*,java.util.*,org.jboss.jmx.adaptor.model.*,java.io.*"%>
<%!
 
   /**
    * Translate HTML tags and single and double quotes.
    */
   public String translateMetaCharacters(Object value)
   {
      if(value == null) 
         return null;
          
      String s = String.valueOf(value);   
      String sanitizedName = s.replace("<", "&lt;");
      sanitizedName = sanitizedName.replace(">", "&gt;");
      sanitizedName = sanitizedName.replace("\"", "&quot;");
      sanitizedName = sanitizedName.replace("\'", "&apos;");
      return sanitizedName;
   }
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%
	 String bindAddress = "";
         String serverName = "";
         try
         {
            bindAddress = System.getProperty("jboss.bind.address", "");
            serverName = System.getProperty("jboss.server.name", "");
         }
         catch (SecurityException se) {}

         String hostname = "";
         try
         {
            hostname = InetAddress.getLocalHost().getHostName();
         }
         catch(IOException e)  {}

         String hostInfo = hostname;
         if (!bindAddress.equals(""))
         {
            hostInfo = hostInfo + " (" + bindAddress + ")";
         }
%>
<html>
<head>
   <title>JBoss JMX Management Console - <%= hostInfo %></title>
   <link rel="stylesheet" href="style_master.css" type="text/css"/>
   <meta http-equiv="cache-control" content="no-cache"/>
</head>

<body>

  <table width='100%' cellspacing="0" cellpadding="0" border="0">
    <tr>
      <td height="105" align="center"><h1>JMX Agent View</h1><h3><%= hostInfo%> - <%= serverName %></h3></td>
      <td height="105" align="center" width="300" nowrap>
        <form action="HtmlAdaptor?action=displayMBeans" method="post" name="applyFilter" id="applyFilter">
          ObjectName Filter (e.g.: "jboss:*", "*:service=invoker,*"):<br/>
          <input type="text" name="filter" size="20" value="<%= request.getAttribute("filter")%>" />
          <input type="submit" name="apply" value="Apply Filter" />
          <input type="button" onClick="javascript:location='HtmlAdaptor?filter='" value="Clear Filter" />
<%
	if (request.getAttribute("filterError") != null) {
                out.println("<br/><span class='error'>"+request.getAttribute("filterError")+"</span>");
        }
%>
        </form>
        <%= new java.util.Date() %>
      </td>
    </tr>
  </table>

  &nbsp;

<%
   out.println("<table width='100%' cellspacing='1' cellpadding='1' border='1'>");
   Iterator mbeans = (Iterator) request.getAttribute("mbeans");
   int i=0;
   while( mbeans.hasNext() )
   {
      DomainData domainData = (DomainData) mbeans.next();
      out.println(" <tr>");
      out.println("  <th style='text-align: left'>");
      out.println("   <h2><a href=\"javascript:document.applyFilter.filter.value='"+domainData.getDomainName()+":*';document.applyFilter.submit()\">"+domainData.getDomainName()+"</a></h2>");
      out.println("  </th>");
      out.println(" </tr>");
      out.println(" <tr>");
      out.println("  <td bgcolor='#D0D0D0'>");
      out.println("    <ul>");
      MBeanData[] data = domainData.getData();
      for(int d = 0; d < data.length; d ++)
      {
         String name = data[d].getObjectName().toString();
         String properties = translateMetaCharacters(data[d].getNameProperties());
         out.println("     <li><a href=\"HtmlAdaptor?action=inspectMBean&amp;name="+URLEncoder.encode(name,"UTF-8")+"\">"+URLDecoder.decode(properties,"UTF-8")+"</a></li>");
      }
      out.println("   </ul>");
      out.println("  </td>");
      out.println(" </tr>");
   }
   out.println("</table>");
%>

</body>
</html>
