<%@ page import="org.wildfly.test.integration.jsp.DummyConstants" %>
<%@ page import="org.wildfly.test.integration.jsp.DummyEnum" %>

<html>
   <body>
      Boolean.TRUE: --- ${Boolean.TRUE} ---<br/>
      Integer.MAX_VALUE: --- ${Integer.MAX_VALUE} ---<br/>
      DummyConstants.FOO: --- ${DummyConstants.FOO} ---<br/>
      DummyEnum.VALUE: --- ${DummyEnum.VALUE} ---<br/>
   </body>
</html>