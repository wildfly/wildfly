package org.jboss.as.test.integration.ejb.access.log;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.ejb.access.log.util.AccessLog;
import org.jboss.as.test.integration.ejb.access.log.util.AccessLogFormat;
import org.jboss.as.test.integration.ejb.access.log.util.EJBUtil;
import org.jboss.as.test.integration.ejb.access.log.util.ServerLog;
import org.jboss.as.test.integration.ejb.access.log.util.ServerStdout;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.naming.InitialContext;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 *     Tests for [ WFLY-6892 ].
 * </p>
 *
 * <p>
 *     Base class for EJB access log tests: contains the actual tests being executed;
 *     These same tests get executed in different context: when console log is enabled, when server log is enabled, when
 *     file log is enabled or a combination of them ...
 * </p>
 * <p>
 *     Various kinds of EJBs are deployed on the server and then accessed locally, remotely and via HTTP;
 *     After each access the relevant logs are verified;
 * </p>
 *
 * @author <a href="mailto:tborgato@redhat.com">Tommaso Borgato</a>
 */
public abstract class AbstractAccessLogTestCase {
    protected static final Logger logger = Logger.getLogger(AbstractAccessLogTestCase.class);
    protected static final String APP_NAME = "ejb-access-logs-test-app";
    protected static final String MODULE_NAME_EJB = "ejb-access-logs-test-app-ejb";
    protected static final String MODULE_NAME_WAR = "ejb-access-logs-test-app-web";
    protected static final String SERVER_LOG_FILE = "server.log";
    protected static final String EJB_ACCESS_LOG_FILE = "ejb-access.log";

    @ArquillianResource
    protected URL contextPath;

    @ArquillianResource
    protected ManagementClient mgmtClient;

    /**
     * Used to verify wether or not an access log is valid JSON
     * @param accessLogs
     * @return
     */
    protected Collection<JsonObject> getJSON(List<AccessLog> accessLogs) {
        final Collection<JsonObject> result = new ArrayList<>();
        for (AccessLog line : accessLogs) {
            try (JsonReader reader = Json.createReader(new StringReader(line.getLine()))) {
                final JsonObject jsonObject = reader.readObject();
                result.add(jsonObject);
            } catch (Exception ignore) {}
        }
        return result;
    }

    /**
     * Subclasses have ti implement the check that must be tailored to the actual access log configuration
     * @param ejbInterface
     * @param ejbClass
     * @param ejbMethod
     * @param user
     * @throws IOException
     * @throws InterruptedException
     */
    protected abstract void checkAccessLog(Class ejbInterface, Class ejbClass, String ejbMethod, String user) throws IOException, InterruptedException;

    /**
     * Creates a deployment with one EJB module and one WEB module which accesses the Stateles EJB through a servlet
     * @param classes
     * @return
     */
    public static Archive createDeployment(Class ... classes) {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");

        final JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "bean-interfaces.jar")
                .addClasses(AccessLog.class, AccessLogFormat.class, ServerStdout.class, ServerLog.class, TimeoutUtil.class)
                .addClasses(CLIServerSetupTask.class, ServerSetupTask.class)
                .addClasses(Util.class, AbstractAccessLogTestCase.class)
                .addClasses(classes);
        ear.addAsLibrary(libJar);

        final JavaArchive ejbJarOne = ShrinkWrap.create(JavaArchive.class, MODULE_NAME_EJB + ".jar")
                .addClasses(SLSBRemote.class, SLSBLocal.class, SLSB.class)
                .addClasses(SFSBRemote.class, SFSBLocal.class, SFSB.class);
        ear.addAsModule(ejbJarOne);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME_WAR + ".war")
                .addClasses(SLSBLocalServlet.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(new StringAsset(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<web-app version=\"3.1\"\n" +
                                "         xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"\n" +
                                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "         xsi:schemaLocation=\"\n" +
                                "      http://xmlns.jcp.org/xml/ns/javaee\n" +
                                "      http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\">\n" +
                                "</web-app>\n"), "web.xml")
                ;
        ear.addAsModule(war);
        return ear;
    }

    /*
        TODO: delete this method that is just for developing purposes
     */
    @Deprecated
    protected void appendToFile(String filename, String[] lines) {
        StringBuilder chunck = new StringBuilder();
        for (String line : lines) {
            chunck.append(line);
            chunck.append("\n");
        }
        appendToFile(filename, chunck.toString());
    }

    /*
        TODO: delete this method that is just for developing purposes
     */
    @Deprecated
    protected void appendToFile(String filename, String chunck) {
        //TODO: remove this code
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(
                    new FileWriter(filename, true)  //Set true for append mode
            );
            writer.newLine();
            writer.write("\n\n<SERVER_LOG_CHUNK>\n" + chunck + "\n</SERVER_LOG_CHUNK>\n\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            if (writer != null) try {
                writer.close();
            } catch (Exception ignore) {
            }
        }
    }

    private boolean containsAllStrings(String line, String... specificStrings) {
        if (line == null) return false;
        for (String str : specificStrings) {
            if (str != null && !line.contains(str)) return false;
        }
        return true;
    }

    /* ==============================================
                    LOG EXTRACTORS
       ============================================== */

    protected List<AccessLog> getAccessLogs(String chunck, AccessLogFormat accessLogFormat, String... specificStrings) {
        List<AccessLog> retval = null;
        if (chunck != null) {
            String[] lines = chunck.split("\\r?\\n");
            retval = getAccessLogs(lines, accessLogFormat, specificStrings);
        }
        return retval;
    }

    protected List<AccessLog> getAccessLogs(String[] lines, AccessLogFormat accessLogFormat, String... specificStrings) {
        List<AccessLog> retval = null;
        if (lines != null && lines.length > 0) {
            retval = new ArrayList<>();
            for (String line : lines) {
                Matcher matcher = accessLogFormat.getRegexp().matcher(line);
                if (
                        containsAllStrings(line, specificStrings)
                                &&
                                matcher.matches()
                ) {
                    AccessLog accessLog = new AccessLog(line);
                    retval.add(accessLog);
                }
            }
        }
        return retval;
    }

    /* ==============================================
         Utilities for storing log files info in
         temporary files in order to persist these
         info across subsequent tests
       ============================================== */

    protected Path getTmpFilePath(String suffix) {
        return Paths.get(System.getProperty("java.io.tmpdir"), String.format("%s-%s.tmp", this.getClass().getSimpleName(), suffix.replace(".log","")));
    }

    protected void writeTmpFile(String suffix, Path path, long offset) throws IOException {
        Path tmp = getTmpFilePath(suffix);
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmp.toFile(), false));
        writer.write(String.format("%s:%d", path.toFile().getAbsolutePath(), offset));
        writer.close();

        System.out.println("\n\twrote to " + getTmpFilePath(suffix) + " " + path + ":" + offset + "\n");
    }

    protected Map.Entry<Path, Long> getTmpFileContent(String suffix) throws IOException {
        List<String> lines = Files.readAllLines(getTmpFilePath(suffix));
        Path path = Paths.get(lines.get(0).split(":")[0]);
        Long offset = Long.parseLong(lines.get(0).split(":")[1]);

        System.out.println("\n\tread from " + getTmpFilePath(suffix) + " " + path + ":" + offset + "\n");

        return new AbstractMap.SimpleEntry<>(path, offset);
    }

    /**
     * Executes an HTTP call against the current server;
     * E.g. to invoke {@link SLSBLocalServlet}
     *
     * @param path
     * @return
     * @throws Exception
     */
    protected String doGetRequest(String path) throws Exception {
        logger.info("Invoking URL " + contextPath + path);
        return HttpRequest.get(contextPath + path, 10, TimeUnit.SECONDS);
    }

    /**
     * <p>
     *     Defines the path to the log files we are testing (the log files supposed to contain EJB access logs);
     *     Log files are positioned inside WildFly log directory;
     * </p>
     * Note:
     * <ul>
     *     <li>
     *         RunAsClient=TRUE: we can use env variable <b>jboss.home</b>
     *     </li>
     *     <li>
     *         RunAsClient=FALSE: we can use env variable <b>jboss.home.dir</b>
     *     </li>
     * </ul>
     *
     * @param fileName e.g. server.log
     * @return e.g. jboss-as/standalone/log/server.log
     */
    protected Path getLogFilePath(String fileName) {
        String jbossHome = System.getProperty("jboss.home", null);
        return Paths.get(
                (jbossHome != null ? jbossHome : System.getProperty("jboss.home.dir", null)), "standalone", "log", fileName
        );
    }

    /* ==============================================
                ATTRIBUTES
       ============================================== */

    @Test
    @InSequence(1)
    public void testAllAttributes() throws Exception {
        /*
        final ModelNode op = Operations.createAddress("subsystem", "ejb3", "service", "access-log");
        final ModelNode attributes = op.get("attributes");
        for (String name : ATTRIBUTE_NAMES) {
            System.out.println("\n name=" + name + "\n");
        }
        */
        // TODO: .. verify each element and attribute can be created, read, updated, and deleted (CRUD)
        // TODO: .. verify default values for each element and attribute
    }

    /* ==============================================
                LOCAL EJB CLIENT TESTS
       ============================================== */

    /**
     * Access unsecured Stateless EJB method
     */
    @Test
    @InSequence(2)
    public void testSLSB() throws Exception {
        Callable<Void> callable = () -> {
            final SLSBRemote allowAccessBean = InitialContext.doLookup("java:global/" + APP_NAME + "/" + MODULE_NAME_EJB + "/" + SLSB.class.getSimpleName() + "!" + SLSBRemote.class.getName());
            final String echo = allowAccessBean.echo("TUTTO A POSTO A FERRAGOSTO");
            Assert.assertTrue("Could not access unsecured SLSB", echo != null && echo.contains("TUTTO A POSTO A FERRAGOSTO"));
            return null;
        };
        // establish an identity using the security domain associated with the beans in the JARs in the EAR deployment
        Util.switchIdentity(null, null, callable, SLSB.class.getClassLoader());
        checkAccessLog(SLSBRemote.class, SLSB.class, "echo", null);
    }

    /**
     * Access unsecured Stateful EJB method
     */
    @Test
    @InSequence(3)
    public void testSFSB() throws Exception {
        Callable<Void> callable = () -> {
            final SFSBRemote allowAccessBean = InitialContext.doLookup("java:global/" + APP_NAME + "/" + MODULE_NAME_EJB + "/" + SFSB.class.getSimpleName() + "!" + SFSBRemote.class.getName());
            final String echo = allowAccessBean.echo("TUTTO A POSTO A FERRAGOSTO");
            Assert.assertTrue("Could not access unsecured SFSB", echo != null && echo.contains("TUTTO A POSTO A FERRAGOSTO"));
            return null;
        };
        // establish an identity using the security domain associated with the beans in the JARs in the EAR deployment
        Util.switchIdentity(null, null, callable, SFSB.class.getClassLoader());
        checkAccessLog(SFSBRemote.class, SFSB.class, "echo", null);
    }

    /**
     * Access secured Stateless EJB method
     */
    @Test
    @InSequence(4)
    public void testSLSBSecured() throws Exception {
        Callable<Void> callable = () -> {
            final SLSBRemote allowAccessBean = InitialContext.doLookup("java:global/" + APP_NAME + "/" + MODULE_NAME_EJB + "/" + SLSB.class.getSimpleName() + "!" + SLSBRemote.class.getName());
            final String echo = allowAccessBean.echoSecuredRole1("TUTTO A POSTO A FERRAGOSTO");
            Assert.assertTrue("Could not access unsecured SLSB", echo != null && echo.contains("TUTTO A POSTO A FERRAGOSTO"));
            return null;
        };
        Util.switchIdentity("user1", "password1", callable, SLSB.class.getClassLoader());
        checkAccessLog(SLSBRemote.class, SLSB.class, "echoSecuredRole1", "user1");
    }

    /**
     * Access secured Stateful EJB method
     */
    @Test
    @InSequence(5)
    public void testSFSBSecured() throws Exception {
        Callable<Void> callable = () -> {
            final SFSBRemote allowAccessBean = InitialContext.doLookup("java:global/" + APP_NAME + "/" + MODULE_NAME_EJB + "/" + SFSB.class.getSimpleName() + "!" + SFSBRemote.class.getName());
            final String echo = allowAccessBean.echoSecuredRole2("TUTTO A POSTO A FERRAGOSTO");
            Assert.assertTrue("Could not access role secured SFSB", echo != null && echo.contains("TUTTO A POSTO A FERRAGOSTO"));
            return null;
        };
        Util.switchIdentity("user2", "password2", callable, SFSB.class.getClassLoader());
        checkAccessLog(SFSBRemote.class, SFSB.class, "echoSecuredRole2", "user2");
    }

    /* ==============================================
                REMOTE EJB CLIENT TESTS
       ============================================== */

    /**
     * Remote access unsecured Stateless EJB method
     */
    @Test
    @InSequence(6)
    @RunAsClient
    public void testRemoteSLSB() throws Exception {
        final Properties ejbClientConfiguration = EJBUtil.createEjbClientConfiguration(Utils.getHost(mgmtClient), null, null);
        final SLSBRemote targetBean = EJBUtil.lookupEJB(SLSB.class, SLSBRemote.class, ejbClientConfiguration, APP_NAME, MODULE_NAME_EJB, false);
        String echo = targetBean.echo("TUTTO A POSTO A FERRAGOSTO");
        Assert.assertTrue("Could not access role secured SLSB", echo != null && echo.contains("TUTTO A POSTO A FERRAGOSTO"));
        checkAccessLog(SLSBRemote.class, SLSB.class, "echo", null);
    }

    /**
     * Remote access secured Stateless EJB method
     */
    @Test
    @InSequence(7)
    @RunAsClient
    public void testRemoteSLSBSecured() throws Exception {
        final Properties ejbClientConfiguration = EJBUtil.createEjbClientConfiguration(Utils.getHost(mgmtClient), "user1", "password1");
        final SLSBRemote targetBean = EJBUtil.lookupEJB(SLSB.class, SLSBRemote.class, ejbClientConfiguration, APP_NAME, MODULE_NAME_EJB, false);
        String echo = targetBean.echoSecuredRole1("TUTTO A POSTO A FERRAGOSTO");
        Assert.assertTrue("Could not access role secured SLSB", echo != null && echo.contains("TUTTO A POSTO A FERRAGOSTO"));
        checkAccessLog(SLSBRemote.class, SLSB.class, "echoSecuredRole1", "user1");
    }

    /**
     * Remote access unsecured Stateful EJB method
     */
    @Test
    @InSequence(8)
    @RunAsClient
    public void testRemoteSFSB() throws Exception {
        final Properties ejbClientConfiguration = EJBUtil.createEjbClientConfiguration(Utils.getHost(mgmtClient), null, null);
        final SFSBRemote targetBean = EJBUtil.lookupEJB(SFSB.class, SFSBRemote.class, ejbClientConfiguration, APP_NAME, MODULE_NAME_EJB, true);
        String echo = targetBean.echo("TUTTO A POSTO A FERRAGOSTO");
        Assert.assertTrue("Could not access role secured SFSB", echo != null && echo.contains("TUTTO A POSTO A FERRAGOSTO"));
        checkAccessLog(SFSBRemote.class, SFSB.class, "echo", null);
    }

    /**
     * Remote access secured Stateful EJB method
     */
    @Test
    @InSequence(9)
    @RunAsClient
    public void testRemoteSFSBSecured() throws Exception {
        final Properties ejbClientConfiguration = EJBUtil.createEjbClientConfiguration(Utils.getHost(mgmtClient), "user1", "password1");
        final SFSBRemote targetBean = EJBUtil.lookupEJB(SFSB.class, SFSBRemote.class, ejbClientConfiguration, APP_NAME, MODULE_NAME_EJB, true);
        String echo = targetBean.echoSecuredRole1("TUTTO A POSTO A FERRAGOSTO");
        Assert.assertTrue("Could not access role secured SFSB", echo != null && echo.contains("TUTTO A POSTO A FERRAGOSTO"));
        checkAccessLog(SFSBRemote.class, SFSB.class, "echoSecuredRole1", "user1");
    }

    /* ==============================================
                HTTP TEST
       ============================================== */

    @Test
    @InSequence(10)
    @RunAsClient
    public void testSLSBLocalServlet() throws Exception {
        String result = doGetRequest("/slsblocal");
        assertEquals("ECHO[TUTTO A POSTO A FERRAGOSTO]", result);
        checkAccessLog(SLSBRemote.class, SLSB.class, "echo", null);
    }
}
