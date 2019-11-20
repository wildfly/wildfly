<%--
  Copyright 2019 Red Hat, Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
--%>
<%@ page import="org.wildfly.test.integration.jsp.DummyConstants" %>
<%@ page import="org.wildfly.test.integration.jsp.DummyEnum" %>
<%@ page import="org.wildfly.test.integration.jsp.DummyBean" %>

<html>
   <body>
      Boolean.TRUE: --- ${Boolean.TRUE} ---<br/>
      Integer.MAX_VALUE: --- ${Integer.MAX_VALUE} ---<br/>
      DummyConstants.FOO: --- ${DummyConstants.FOO} ---<br/>
      DummyEnum.VALUE: --- ${DummyEnum.VALUE} ---<br/>
      <jsp:useBean id="DummyBean" class="org.wildfly.test.integration.jsp.DummyBean"/>
      DummyBean.test: --- ${DummyBean.test} ---</br>
   </body>
</html>