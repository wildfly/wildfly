package org.jboss.as.test.integration.ejb.access.log;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.ejb.access.log.util.AccessLog;
import org.jboss.as.test.integration.ejb.access.log.util.AccessLogFormat;
import org.jboss.as.test.integration.ejb.access.log.util.ServerLog;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * This test case focuses on ejb access logs written to multiple locations:
 * <ul>
 *     <li>the server console output</li>
 *     <li>server log file "standalone/log/server.log"</li>
 *     <li>custom log file "standalone/log/ejb-access.log"</li>
 * </ul>
 *
 * @author tborgato <a href="mailto:tborgato@redhat.com">Tommaso Borgato</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(ConsoleAndServerLogAndFileAccessLogTestCase.EjbAccessLogSetupTask.class)
public class ConsoleAndServerLogAndFileAccessLogTestCase extends AbstractConsoleAccessLogTestCase {
    private static final AccessLogFormat ACCESS_LOG_FORMAT = AccessLogFormat.LONG;

    /* ==============================================
                        PRE-REQUISITE
       ============================================== */

    /**
     * Retrieve the path to server and custom log files and store their location so that it can be used by tests run in container / out of container
     */
    @Test
    @InSequence(Integer.MIN_VALUE)
    @RunAsClient
    public void setTmpFiles() throws IOException {
        Path serverLogPath = getLogFilePath(SERVER_LOG_FILE);
        Assert.assertTrue(String.format("Server log file '%s' does not exist!", serverLogPath.toFile().getAbsolutePath()), serverLogPath.toFile().exists());
        writeTmpFile(
                SERVER_LOG_FILE,
                serverLogPath,
                serverLogPath.toFile().length() // discard output generated so far
        );
        Path ejbAccessLogFilePath = getLogFilePath(EJB_ACCESS_LOG_FILE);
        Assert.assertFalse(String.format("EJB access log file '%s' already exist!", ejbAccessLogFilePath.toFile().getAbsolutePath()), ejbAccessLogFilePath.toFile().exists());
        writeTmpFile(
                EJB_ACCESS_LOG_FILE,
                ejbAccessLogFilePath,
                ejbAccessLogFilePath.toFile().length() // discard output generated so far
        );
    }

    @Test
    @InSequence(Integer.MAX_VALUE)
    @RunAsClient
    public void removeTmpFiles() {
        // remove the tmp file containing the location and offset of the actual log files
        Assert.assertTrue(getTmpFilePath(SERVER_LOG_FILE).toFile().delete());
        Assert.assertTrue(getTmpFilePath(EJB_ACCESS_LOG_FILE).toFile().delete());
        // remove the custom log file
        Assert.assertTrue(getLogFilePath(EJB_ACCESS_LOG_FILE).toFile().delete());
    }

    @Deployment
    public static Archive createDeployment() {
        return createDeployment(ConsoleAndServerLogAndFileAccessLogTestCase.EjbAccessLogSetupTask.class, AbstractConsoleAccessLogTestCase.class, ConsoleAndServerLogAndFileAccessLogTestCase.class);
    }

    private void checkAccessLogConsole(Class ejbInterface, Class ejbClass, String ejbMethod, String user) {
        String[] lines = serverStdout.getNewLines();
        Assert.assertNotNull("No access log messages generated in server console!", lines);

        //TODO: remove this code
        appendToFile("/tmp/ConsoleAndServerLogAndFileAccessLogTestCase.txt", lines);

        // get access logs
        List<AccessLog> accessLogs = getAccessLogs(lines, ACCESS_LOG_FORMAT, ejbInterface.getSimpleName(), ejbClass.getSimpleName(), ejbMethod, user);

        Assert.assertTrue("EJB access log not found in console!", accessLogs != null && accessLogs.size() == 1);
    }

    private void checkAccessLogServerLog(Class ejbInterface, Class ejbClass, String ejbMethod, String user) throws IOException, InterruptedException {
        // read the tmp file holding location and offset of the server log file
        Map.Entry<Path, Long> entry = getTmpFileContent(SERVER_LOG_FILE.replace(".log", ""));
        // access the server log file
        ServerLog serverLog = new ServerLog(entry.getKey(), entry.getValue());
        // and read the chunk added since the last read
        String[] lines = serverLog.getNewLines();

        //TODO: remove this code
        appendToFile("/tmp/ConsoleAndServerLogAndFileAccessLogTestCase.txt", lines);

        Assert.assertNotNull("No access log messages generated in server log file!", lines);

        // get access logs in the expected number and format
        List<AccessLog> accessLogs = getAccessLogs(lines, ACCESS_LOG_FORMAT, ejbInterface.getSimpleName(), ejbClass.getSimpleName(), ejbMethod, user);

        // mark where the log file was last accessed
        writeTmpFile(SERVER_LOG_FILE.replace(".log", ""), entry.getKey(), serverLog.getOffset());

        Assert.assertTrue("EJB access log not found in " + SERVER_LOG_FILE + "!", accessLogs != null && accessLogs.size() == 1);
    }

    private void checkAccessLogFile(Class ejbInterface, Class ejbClass, String ejbMethod, String user) throws IOException, InterruptedException {
        // read the tmp file holding location and offset of the server log file
        Map.Entry<Path, Long> entry = getTmpFileContent(EJB_ACCESS_LOG_FILE.replace(".log", ""));
        // access the server log file
        ServerLog serverLog = new ServerLog(entry.getKey(), entry.getValue());
        // and read the chunk added since the last read
        String[] lines = serverLog.getNewLines();

        //TODO: remove this code
        appendToFile("/tmp/ConsoleAndServerLogAndFileAccessLogTestCase.txt", lines);

        Assert.assertNotNull("No access log messages generated in custom log file!", lines);

        // get access logs
        List<AccessLog> accessLogs = getAccessLogs(lines, ACCESS_LOG_FORMAT, ejbInterface.getSimpleName(), ejbClass.getSimpleName(), ejbMethod, user);

        // mark where the log file was last accessed
        writeTmpFile(EJB_ACCESS_LOG_FILE.replace(".log", ""), entry.getKey(), serverLog.getOffset());

        Assert.assertTrue("EJB access log not found in file " + EJB_ACCESS_LOG_FILE + "!", accessLogs != null && accessLogs.size() == 1);
    }

    @Override
    protected void checkAccessLog(Class ejbInterface, Class ejbClass, String ejbMethod, String user) throws IOException, InterruptedException {
        checkAccessLogConsole(ejbInterface, ejbClass, ejbMethod, user);
        checkAccessLogServerLog(ejbInterface, ejbClass, ejbMethod, user);
        checkAccessLogFile(ejbInterface, ejbClass, ejbMethod, user);
    }

        /* ==============================================
                        Server config
       ============================================== */

    static class EjbAccessLogSetupTask implements ServerSetupTask {

        ModelNode address, operation, result;

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            System.out.println("\n\nsetup\n\n");

            // /subsystem=ejb3/service=access-log:add
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");

            operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            result = managementClient.getControllerClient().execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Can't configure server: " + result.asString());
            }

            // /subsystem=ejb3/service=access-log/pattern-formatter=p1:add(name=p1, pattern=\"short\")
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");
            address.add("pattern-formatter", "p1");

            operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("name").set("p1");
            // short: date time ip user ejb method
            // e.g. 2019-05-05 12:23:27,003 127.0.0.1 admin hello/helloBean hello
            operation.get("pattern").set(ACCESS_LOG_FORMAT.getPattern());
            result = managementClient.getControllerClient().execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Can't configure server: " + result.asString());
            }

            // /subsystem=ejb3/service=access-log/console-handler=console1:add(name=console1,formatter=p1)
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");
            address.add("console-handler", "console1");

            operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("name").set("console1");
            operation.get("formatter").set("p1");
            result = managementClient.getControllerClient().execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Can't configure server: " + result.asString());
            }

            // /subsystem=ejb3/service=access-log/server-log-handler=server1:add(name=server1,formatter=p1)
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");
            address.add("server-log-handler", "server1");

            operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("name").set("server1");
            operation.get("formatter").set("p1");
            result = managementClient.getControllerClient().execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Can't configure server: " + result.asString());
            }

            // /subsystem=ejb3/service=access-log/file-handler=file1:add(name=file1,path=" + EJB_ACCESS_LOG_FILE + ",relative-to=jboss.server.log.dir,formatter=j1)
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");
            address.add("file-handler", "file1");

            operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("name").set("file1");
            operation.get("formatter").set("p1");
            operation.get("path").set(EJB_ACCESS_LOG_FILE);
            operation.get("relative-to").set("jboss.server.log.dir");
            result = managementClient.getControllerClient().execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Can't configure server: " + result.asString());
            }

            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient(), 50000);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            System.out.println("\n\ntearDown\n\n");

            // /subsystem=ejb3/service=access-log/console-handler=console1:remove
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");
            address.add("console-handler", "console1");

            operation = new ModelNode();
            operation.get(OP).set(REMOVE);
            operation.get(OP_ADDR).set(address);
            result = managementClient.getControllerClient().execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Can't configure server: " + result.asString());
            }

            // /subsystem=ejb3/service=access-log/server-log-handler=server1:remove
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");
            address.add("server-log-handler", "server1");

            operation = new ModelNode();
            operation.get(OP).set(REMOVE);
            operation.get(OP_ADDR).set(address);
            result = managementClient.getControllerClient().execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Can't configure server: " + result.asString());
            }

            // /subsystem=ejb3/service=access-log/file-handler=file1:remove
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");
            address.add("file-handler", "file1");

            operation = new ModelNode();
            operation.get(OP).set(REMOVE);
            operation.get(OP_ADDR).set(address);
            result = managementClient.getControllerClient().execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Can't configure server: " + result.asString());
            }

            // /subsystem=ejb3/service=access-log/pattern-formatter=p1:remove
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");
            address.add("pattern-formatter", "p1");

            operation = new ModelNode();
            operation.get(OP).set(REMOVE);
            operation.get(OP_ADDR).set(address);
            result = managementClient.getControllerClient().execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Can't configure server: " + result.asString());
            }

            // /subsystem=ejb3/service=access-log:remove
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");

            operation = new ModelNode();
            operation.get(OP).set(REMOVE);
            operation.get(OP_ADDR).set(address);
            result = managementClient.getControllerClient().execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Can't configure server: " + result.asString());
            }

            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient(), 50000);
        }
    }
}
