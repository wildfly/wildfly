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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.json.JsonObject;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * This test case focuses on ejb access logs written to server log file "standalone/log/server.log" in JSON format
 *
 * @author tborgato <a href="mailto:tborgato@redhat.com">Tommaso Borgato</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(ServerLogAccessLogJsonTestCase.EjbAccessLogSetupTask.class)
public class ServerLogAccessLogJsonTestCase extends AbstractAccessLogTestCase {
    private static final AccessLogFormat ACCESS_LOG_FORMAT = AccessLogFormat.SHORT_JSON;

    /**
     * Retrieve the path to server log file and store it's location so that it can be used by tests run in container / out of container
     */
    @Test
    @InSequence(-1)
    @RunAsClient
    public void setTmpFileForServerLogFile() throws IOException {
        Path serverLogPath = getLogFilePath(SERVER_LOG_FILE);
        Assert.assertTrue(String.format("Server log file '%s' does not exist!", serverLogPath.toFile().getAbsolutePath()), serverLogPath.toFile().exists());
        writeTmpFile(
                SERVER_LOG_FILE,
                serverLogPath,
                serverLogPath.toFile().length() // discard output generated so far
        );
    }

    @Test
    @InSequence(Integer.MAX_VALUE)
    @RunAsClient
    public void removeTmpFile() {
        Assert.assertTrue(getTmpFilePath(SERVER_LOG_FILE).toFile().delete());
    }

    // mvn -Dtest=ServelLogAccessLogTestCase clean test

    /**
     * Deploy one Stateful EJB and one Stateless EJB both with a local and remote interface;
     * Both EJBs have some methods secured;
     *
     * @return
     */
    @Deployment
    public static Archive createDeployment() {
        return createDeployment(ServerLogAccessLogJsonTestCase.EjbAccessLogSetupTask.class, ServerLogAccessLogJsonTestCase.class);
    }

    @Override
    protected void checkAccessLog(Class ejbInterface, Class ejbClass, String ejbMethod, String user) throws IOException, InterruptedException {
        // read the tmp file holding location and offset of the server log file
        Map.Entry<Path, Long> entry = getTmpFileContent(SERVER_LOG_FILE.replace(".log", ""));
        // access the server log file
        ServerLog serverLog = new ServerLog(entry.getKey(), entry.getValue());
        // and read the chunk added since the last read
        String[] lines = serverLog.getNewLines();

        Assert.assertNotNull("No access log messages generated in server log file!", lines);

        // get access logs in the expected number and format
        List<AccessLog> accessLogs = getAccessLogs(lines, ACCESS_LOG_FORMAT, ejbInterface.getSimpleName(), ejbClass.getSimpleName(), ejbMethod, user);

        Assert.assertTrue("JSON EJB access log not found!", accessLogs != null && accessLogs.size() == 1);

        // mark where the log file was last accessed
        writeTmpFile(SERVER_LOG_FILE.replace(".log", ""), entry.getKey(), serverLog.getOffset());

        Collection<JsonObject> jsons = getJSON(accessLogs);

        Assert.assertTrue("EJB access log not in JSON format!", jsons != null && jsons.size() == 1);
    }

    /* ==============================================
                        Server config
       ============================================== */

    static class EjbAccessLogSetupTask implements ServerSetupTask {

        ModelNode address, operation, result;

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            // /subsystem=ejb3/service=access-log:add(destination=logging)
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");

            operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("destination").set("logging");
            operation.get("include-local").set(true);
            result = managementClient.getControllerClient().execute(operation);
            if (!Operations.isSuccessfulOutcome(result)) {
                throw new Exception("Can't configure server: " + result.asString());
            }

            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient(), 50000);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
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
