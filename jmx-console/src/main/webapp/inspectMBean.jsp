<?xml version="1.0"?>
<%@page contentType="text/html"
   import="java.net.*,java.io.*,java.util.*,javax.management.*,javax.management.modelmbean.*,
   org.jboss.jmx.adaptor.control.Server,
   org.jboss.jmx.adaptor.control.AttrResultInfo,
   org.jboss.jmx.adaptor.model.*,
   org.dom4j.io.HTMLWriter,
   org.dom4j.tree.FlyweightCDATA,
   java.lang.reflect.Array,
   java.io.StringWriter,
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

<%!
    private static final Comparator MBEAN_FEATURE_INFO_COMPARATOR = new Comparator()
    {
      public int compare(Object value1, Object value2)
      {
        MBeanFeatureInfo featureInfo1 = (MBeanFeatureInfo) value1;
        MBeanFeatureInfo featureInfo2 = (MBeanFeatureInfo) value2;

        String name1 = featureInfo1.getName();
        String name2 = featureInfo2.getName();

        return name1.compareTo(name2);
      }

      public boolean equals(Object other)
      {
        return this == other;
      }
    };

    String sep = System.getProperty("line.separator","\n");

    public String fixDescription(String desc)
    {
      if (desc == null || desc.equals(""))
      {
        return "(no description)";
      }
      return desc;
    }

    public String fixValue(Object value)
    {
        if (value == null)
            return null;
        String s = String.valueOf(value);
        StringWriter sw = new StringWriter();
        HTMLWriter hw = new HTMLWriter(sw);
        try
        {
           // hw.write(s); // strips whitespace
           hw.write(new FlyweightCDATA(s));
	   s = sw.toString();
        }
        catch(Exception e)
        {
        }
        return s;
    }

    public String fixValueForAttribute(Object value)
    {
        if (value == null)
            return null;
      String s = String.valueOf(value);
       StringWriter sw = new StringWriter();
       HTMLWriter hw = new HTMLWriter(sw);
       try
       {
          hw.write(s);
          s = sw.toString();
       }
       catch(Exception e)
       {
       }
       return s;
    }
    
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

<!DOCTYPE html 
    PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>

<head>
   <title>MBean Inspector</title>
   <link rel="stylesheet" href="style_master.css" type="text/css" />
   <meta http-equiv="cache-control" content="no-cache" />
</head>

<jsp:useBean id='mbeanData' class='org.jboss.jmx.adaptor.model.MBeanData' scope='request'/>
<%
   if(mbeanData.getObjectName() == null)
   {
%>
<jsp:forward page="/" />
<%
   }
   ObjectName objectName = mbeanData.getObjectName();
   String objectNameString = mbeanData.getName();
   String quotedObjectNameString = URLEncoder.encode(mbeanData.getName(), "UTF-8");
   MBeanInfo mbeanInfo = mbeanData.getMetaData();
   MBeanAttributeInfo[] attributeInfo = mbeanInfo.getAttributes();
   MBeanOperationInfo[] operationInfo = mbeanInfo.getOperations();

   //FIXME: Seems to create ArrayIndexOutofBoundsException when uncommented
   /*Arrays.sort(attributeInfo, MBEAN_FEATURE_INFO_COMPARATOR);

   HashMap operationInfoIndexMap = new HashMap();
   for (int a = 0; a < operationInfo.length; a++)
   {
      MBeanOperationInfo opInfo = operationInfo[a];
      operationInfoIndexMap.put(opInfo, String.valueOf(a));
   }

   Arrays.sort(operationInfo, MBEAN_FEATURE_INFO_COMPARATOR);
   */
%>

<body leftmargin="10" rightmargin="10" topmargin="10">

<table width="100%" cellspacing="0" cellpadding="0" border="0" align="center">
 <tr>
  <td height="105" align="center"><h1>JMX MBean View</h1><%= hostname %></td>
  <td height="105" align="center" width="300" nowrap>
    <p>
      <input type="button" value="Back to Agent" onClick="javascript:location='HtmlAdaptor?action=displayMBeans'"/>
      <input type="button" value="Refresh MBean View" onClick="javascript:location='HtmlAdaptor?action=inspectMBean&amp;name=<%= URLEncoder.encode(request.getParameter("name"),"UTF-8") %>'"/>
    </p>
    <%= new java.util.Date() %>
  </td>
 </tr>
</table>

&nbsp;

<%
   Hashtable properties = objectName.getKeyPropertyList();
   int size = properties.keySet().size();
%>

<!-- 1 -->

<table width="100%" cellspacing="1" cellpadding="1" border="1" align="center">
 <tr><th rowspan="<%= size + 1 %>">Name</th><td><b>Domain</b></td><td><%= objectName.getDomain() %></td></tr>
<%
   Iterator it = properties.keySet().iterator();
   while( it.hasNext() )
   {
     String key=(String)it.next();
     String val=translateMetaCharacters((String)properties.get(key));
     out.println(" <tr><td><b>"+key+"</b></td><td>"+val+"</td></tr>");
   }
%>
 <tr><th>Java Class</th><td colspan="2"><jsp:getProperty name='mbeanData' property='className'/></td></tr>
 <tr><th>Description</th><td colspan="2"><%= fixDescription(mbeanInfo.getDescription())%></td></tr>
</table>

<!-- 2 -->
<br/>
<form method="post" action="HtmlAdaptor">
 <input type="hidden" name="action" value="updateAttributes" />
 <input type="hidden" name="name" value="<%= objectNameString %>" />
 <table width="100%" cellspacing="1" cellpadding="1" border="1" align="center">
  <tr>
   <th>Attribute Name</th>
   <th>Access</th>
   <th>Type</th>
   <th>Description</th>
   <th>Attribute Value</th>
  </tr>
<%
  boolean hasWriteableAttribute=false;
  for(int a = 0; a < attributeInfo.length; a ++)
  {
    MBeanAttributeInfo attrInfo = attributeInfo[a];
    String attrName = attrInfo.getName();
    String attrType = attrInfo.getType();
    AttrResultInfo attrResult = Server.getMBeanAttributeResultInfo(objectNameString, attrInfo);
    String attrValue = attrResult.getAsText();
    String access = "";
    if( attrInfo.isReadable() ) access += "R";
    if( attrInfo.isWritable() )
    {
      access += "W";
      hasWriteableAttribute=true;
    }
    String attrDescription = fixDescription(attrInfo.getDescription());
    out.println("  <tr>");
    out.println("   <td class='param'>"+attrName+"</td>");
    out.println("   <td align='center'>"+access+"</td>");
    out.println("   <td>"+attrType+"</td>");
    out.println("   <td>"+attrDescription+"</td>");
    out.println("   <td>");
    out.println("    <pre>");

    if( attrInfo.isWritable() )
    {
      String readonly = attrResult.editor == null ? "class='readonly' readonly" : "class='writable'";
      if( attrType.equals("boolean") || attrType.equals("java.lang.Boolean") )
      {
        Boolean value = attrValue == null || "".equals( attrValue ) ? null : Boolean.valueOf(attrValue);
        String trueChecked = (value == Boolean.TRUE ? "checked" : "");
        String falseChecked = (value == Boolean.FALSE ? "checked" : "");
	String naChecked = value == null ? "checked" : "";
        out.print("<input type='radio' name='"+attrName+"' value='True' "+trueChecked+"/>True");
        out.print("<input type='radio' name='"+attrName+"' value='False' "+falseChecked+"/>False");
	// For wrappers, enable a 'null' selection
	if ( attrType.equals( "java.lang.Boolean" ) && PropertyEditors.isNullHandlingEnabled() )
        {
		out.print("<input type='radio' name='"+attrName+"' value='' "+naChecked+"/>True");
	}

      }
      else if( attrInfo.isReadable() )
      {
	attrValue = fixValueForAttribute(attrValue);
        if (String.valueOf(attrValue).indexOf(sep) == -1)
        {
          out.print("<input type='text' size='80' name='"+attrName+"' value='"+translateMetaCharacters(attrValue)+"' "+readonly+"/>");
        }
        else
        {
          out.print("<textarea cols='80' rows='10' type='text' name='"+attrName+"' "+readonly+">"+attrValue+"</textarea>");
        }
      }
      else
      {
        out.print("<input type='text' name='"+attrName+"' "+readonly+"/>");
      }
    }
    else
    {
      if( attrType.equals("[Ljavax.management.ObjectName;") )
      {
        ObjectName[] names = (ObjectName[]) Server.getMBeanAttributeObject(objectNameString, attrName);
        if( names != null )
        {
          for( int i = 0; i < names.length; i++ )
          {
            out.print("<p align='center'><a href='HtmlAdaptor?action=inspectMBean&name="+URLEncoder.encode(names[i]+"","UTF-8")+">"+names[i]+"</a></p>");
          }
        }
      }
      else if( attrType.startsWith("["))
      {
        Object arrayObject = Server.getMBeanAttributeObject(objectNameString, attrName);
        if (arrayObject != null)
        {
          for (int i = 0; i < Array.getLength(arrayObject); ++i)
          {
            out.println(fixValue(Array.get(arrayObject,i)));
          }
        }
      }
      else
      {
        out.print(fixValue(attrValue));
      }
    }

    if( attrType.equals("javax.management.ObjectName") )
    {
      if( attrValue != null )
      {
        out.print("<p align='center'><a href='HtmlAdaptor?action=inspectMBean&name="+URLEncoder.encode(attrValue,"UTF-8")+"'>View MBean</a></p>");
      }
    }
    out.println("    </pre>");
    out.println("   </td>");
    out.println("  </tr>");
  }

  if(hasWriteableAttribute)
  {
    out.println(" <tr><td colspan='4'></td><td class='arg'><p align='center'><input type='submit' value='Apply Changes'/></p></td></tr>");
  }
%>
 </table>
</form>

<!-- 3 -->
<br/>
<%
if (operationInfo.length > 0)
{
  out.println(" <table width='100%' cellspacing='1' cellpadding='1' border='1' align='center'>");
  out.println("  <tr>");
  out.println("   <th>Operation</th>");
  out.println("   <th>Return Type</th>");
  out.println("   <th>Description</th>");
  out.println("   <th>Parameters</th>");
  out.println("  </tr>");

  for(int a = 0; a < operationInfo.length; a ++)
  {
    MBeanOperationInfo opInfo = operationInfo[a];
    boolean accept = true;
    if (opInfo instanceof ModelMBeanOperationInfo)
    {
      Descriptor desc = ((ModelMBeanOperationInfo)opInfo).getDescriptor();
      String role = (String)desc.getFieldValue("role");
      if ("getter".equals(role) || "setter".equals(role))
      {
        accept = false;
      }
    }
    if (accept)
    {
      MBeanParameterInfo[] sig = opInfo.getSignature();
      out.println("  <tr>");
      out.println("   <td class='param'>"+opInfo.getName()+"</td>");
      out.println("   <td>"+opInfo.getReturnType()+"</td>");
      out.println("   <td>"+fixDescription(opInfo.getDescription())+"</td>");
      out.println("   <td align='center'>");
      out.println("    <form method='post' action='HtmlAdaptor'>");
      out.println("     <input type='hidden' name='action' value='invokeOp'/>");
      out.println("     <input type='hidden' name='name' value='"+quotedObjectNameString+"'/>");
      out.println("     <input type='hidden' name='methodIndex' value='"+a+"'/>");

      if( sig.length > 0 )
      {
        out.println("     <table width='100%' cellspacing='1' cellpadding='1' border='0'>");
        for(int p = 0; p < sig.length; p ++)
        {
          MBeanParameterInfo paramInfo = sig[p];
          String pname = paramInfo.getName();
          String ptype = paramInfo.getType();
          if( pname == null || pname.length() == 0 || pname.equals(ptype) )
          {
            pname = "arg"+p;
          }
          String pdesc = fixDescription(paramInfo.getDescription());
          out.println("      <tr>");
          out.println("       <td class='arg'>"+pname+"</td>");
          out.println("       <td class='arg'>"+ptype+"</td>");
          out.println("       <td class='arg'>"+pdesc+"</td>");
          out.print("       <td class='arg' width='50'>");
          if(ptype.equals("boolean")||ptype.equals("java.lang.Boolean"))
          {
            out.print("<input type='radio' name='arg"+p+"' value='True' checked/>True");
            out.print("<input type='radio' name='arg"+p+"' value='False'/>False");
          }
          else
          {
            out.print("<input type='text' class='writable' name='arg"+p+"'/>");
          }
          out.println("</td>");
          out.println("      </tr>");
        }
        out.println("     </table>");
      }
      else
      {
        out.println("     [no parameters]<BR>");
      }
      out.println("     <input type='submit' value='Invoke'/>");
      out.println("    </form>");
      out.println("  </td>");
      out.println(" </tr>");
    }
  }
  out.println(" </table>");
}
%>

</body>
</html>
