<!-- Tests of JSP page class access
-->
<%@page contentType="text/html" import="java.io.*,java.security.*"%>
<%!private String formatException(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }%>

<html>
<head>
<title>JSP Debug Page</title>
</head>
<body>

	<h2>Classpath Access Tests</h2>
	<h3>Access
		jbosstest-web-util.jar!/org/jboss/as/test/integration/web/classpath/util/Util.class</h3>
	<pre>
<%
    // Access a class from the jbosstest-web-util.jar
    try {
        String className = "org.jboss.as.test.integration.web.classpath.util.Util";
        
        Class<?> clazz = Class.forName(className);
        out.println("Successfully loaded class: " + clazz.getName());
        ClassLoader cl = clazz.getClassLoader();
        ProtectionDomain pd = clazz.getProtectionDomain();
        CodeSource cs = pd.getCodeSource();
        out.println("  ClassLoader : " + cl.getClass().getName() + ':' + cl.hashCode());
        out.println("  CodeSource.location : " + cs.getLocation());
        Class<?> clazz2 = org.jboss.as.test.integration.web.classpath.util.Util.class;
        if (clazz2.equals(clazz) == false)
            throw new ServletException("ClassCastException for " + className + ".class");
        out.println("Static " + clazz2.getName() + ".class matches Class.forName");
    } catch (AccessControlException e) {
        // Ignore security manager related failures.
        e.printStackTrace();
    } catch (Exception e) {
        out.println("Failed");
        String dump = formatException(e);
        out.println(dump);
        response.setHeader("X-Exception", e.getMessage());
    }
%>
</pre>

	<h3>Access
		jbosstest-web-util2.jar!/org/jboss/as/test/integration/web/classpath/util2/Util2.class</h3>
	<pre>
<%
    // Access a class from the jbosstest-web-util2.jar
    try {
        String className = "org.jboss.as.test.integration.web.classpath.util2.Util2";
        
        Class<?> clazz = Class.forName(className);
        out.println("Successfully loaded class: " + clazz.getName());
        ClassLoader cl = clazz.getClassLoader();
        ProtectionDomain pd = clazz.getProtectionDomain();
        CodeSource cs = pd.getCodeSource();
        out.println("  ClassLoader : " + cl.getClass().getName() + ':' + cl.hashCode());
        out.println("  CodeSource.location : " + cs.getLocation());
        Class<?> clazz2 = org.jboss.as.test.integration.web.classpath.util2.Util2.class;
        if (clazz2.equals(clazz) == false)
            throw new ServletException("ClassCastException for " + className + ".class");
        out.println("Static " + clazz2.getName() + ".class matches Class.forName");
    } catch (AccessControlException e) {
        // Ignore security manager related failures.
        e.printStackTrace();
    } catch (Exception e) {
        out.println("Failed");
        String dump = formatException(e);
        out.println(dump);
        response.setHeader("X-Exception", e.getMessage());
    }
%>
</pre>

	<jsp:useBean id="util" scope="session"
		class="org.jboss.as.test.integration.web.classpath.util.Util" />

	<h2>JSP ClassLoaders</h2>
	<pre>
<%
    try {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String loaders = org.jboss.as.test.integration.web.classpath.util.Util.displayClassLoaders(loader);
        out.println(loaders);
    } catch (AccessControlException e) {
        // Ignore security manager related failures.
        e.printStackTrace();
    } catch (Exception e) {
        throw new ServletException();
    }
%>
</pre>

</body>
</html>
