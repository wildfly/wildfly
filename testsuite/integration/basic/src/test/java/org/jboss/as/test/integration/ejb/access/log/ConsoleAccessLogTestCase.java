package org.jboss.as.test.integration.ejb.access.log;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.ejb.access.log.util.AccessLog;
import org.jboss.as.test.integration.ejb.access.log.util.AccessLogFormat;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.runner.RunWith;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * This test case focuses on ejb access logs written to the server console output
 *
 * @author tborgato <a href="mailto:tborgato@redhat.com">Tommaso Borgato</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(ConsoleAccessLogTestCase.EjbAccessLogSetupTask.class)
public class ConsoleAccessLogTestCase extends AbstractConsoleAccessLogTestCase {
    private static final AccessLogFormat ACCESS_LOG_FORMAT = AccessLogFormat.SHORT;

    @Deployment
    public static Archive createDeployment() {
        return createDeployment(ConsoleAccessLogTestCase.EjbAccessLogSetupTask.class, AbstractConsoleAccessLogTestCase.class, ConsoleAccessLogTestCase.class);
    }

    @Override
    protected void checkAccessLog(Class ejbInterface, Class ejbClass, String ejbMethod, String user) throws InterruptedException {
        String[] lines = serverStdout.getNewLines();
        Assert.assertNotNull("No access log messages generated in server console!", lines);

        //TODO: remove this code
        appendToFile("/tmp/ConsoleAccessLogTestCase.txt", lines);

        // get access logs
        List<AccessLog> accessLogs = getAccessLogs(lines, ACCESS_LOG_FORMAT, ejbInterface.getSimpleName(), ejbClass.getSimpleName(), ejbMethod, user);

        Assert.assertTrue("EJB access log not found in console!", accessLogs != null && accessLogs.size() == 1);
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
