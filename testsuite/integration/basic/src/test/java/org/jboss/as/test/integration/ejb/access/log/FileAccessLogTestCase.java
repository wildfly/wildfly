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
 * This test case focuses on ejb access logs written to a custom log file "standalone/log/ejb-access.log"
 *
 * @author tborgato <a href="mailto:tborgato@redhat.com">Tommaso Borgato</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(FileAccessLogTestCase.EjbAccessLogSetupTask.class)
public class FileAccessLogTestCase extends AbstractAccessLogTestCase {
    private static final AccessLogFormat ACCESS_LOG_FORMAT = AccessLogFormat.SHORT_JSON;

    /* ==============================================
                        PRE-REQUISITE
       ============================================== */

    /**
     * Retrieve the path to server log file and store it so info can be shared by tests run in container / out of container
     */
    @Test
    @InSequence(-1)
    @RunAsClient
    public void setTmpFileForEjbAccessLogFile() throws IOException {
        Path ejbAccessLogFilePath = getLogFilePath(EJB_ACCESS_LOG_FILE);
        writeTmpFile(
                EJB_ACCESS_LOG_FILE,
                ejbAccessLogFilePath,
                ejbAccessLogFilePath.toFile().exists() ? ejbAccessLogFilePath.toFile().length() : 0
        );
    }

    @Test
    @InSequence(Integer.MAX_VALUE)
    @RunAsClient
    public void removeTmpFileAndEjbAccessLogFile() throws IOException {
        java.nio.file.Files.deleteIfExists(getTmpFilePath(EJB_ACCESS_LOG_FILE));
        java.nio.file.Files.deleteIfExists(getLogFilePath(EJB_ACCESS_LOG_FILE));
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
        return createDeployment(FileAccessLogTestCase.EjbAccessLogSetupTask.class, FileAccessLogTestCase.class);
    }

    @Override
    protected void checkAccessLog(Class ejbInterface, Class ejbClass, String ejbMethod, String user) throws IOException, InterruptedException {
        // read the tmp file holding location and offset of the server log file
        Map.Entry<Path, Long> entry = getTmpFileContent(EJB_ACCESS_LOG_FILE.replace(".log", ""));
        // verify the custom log file exists
        Assert.assertTrue("File " + entry.getKey() + " not found!", entry.getKey().toFile().exists());
        // access the custom log file
        ServerLog serverLog = new ServerLog(entry.getKey(), entry.getValue());
        // and read the chunk added since the last read
        String[] lines = serverLog.getNewLines();

        Assert.assertNotNull("No access log messages generated in custom log file!", lines);

        // get access logs
        List<AccessLog> accessLogs = getAccessLogs(lines, ACCESS_LOG_FORMAT, ejbInterface.getSimpleName(), ejbClass.getSimpleName(), ejbMethod, user);

        Assert.assertTrue("New ejb access log not found!", accessLogs != null && accessLogs.size() == 1);

        // mark where the log file was last accessed
        writeTmpFile(EJB_ACCESS_LOG_FILE.replace(".log", ""), entry.getKey(), serverLog.getOffset());
    }

        /* ==============================================
                        Server config
       ============================================== */

    static class EjbAccessLogSetupTask implements ServerSetupTask {

        ModelNode address, operation, result;

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            // /subsystem=ejb3/service=access-log:add(destination=file,path=ejb-access.log,relative-to=jboss.server.log.dir)
            address = new ModelNode();
            address.add("subsystem", "ejb3");
            address.add("service", "access-log");

            operation = new ModelNode();
            operation.get(OP).set(ADD);
            operation.get(OP_ADDR).set(address);
            operation.get("destination").set("file");
            operation.get("path").set(EJB_ACCESS_LOG_FILE);
            operation.get("relative-to").set("jboss.server.log.dir");
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
