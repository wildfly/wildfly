package org.jboss.as.test.integration.auditlog;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT_LOG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOG_READ_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.File;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(JmxAuditLogTestCase.JmxAuditLogSetup.class)
public class JmxAuditLogTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    static JMXConnector connector;
    static MBeanServerConnection connection;

    private static File logFile;

    private static final String JMX = "jmx";
    private static final String CONFIGURATION = "configuration";

    @Before
    public void initialize() throws Exception {
        connection = setupAndGetConnection();
    }

    @After
    public void closeConnection() throws Exception {
        IoUtils.safeClose(connector);
    }

    @Test
    public void testLoggingJmxOperation() throws Exception {
        if (logFile.exists()) {
            logFile.delete();
        }
        Assert.assertFalse(logFile.exists());
        makeOneLog();
        Assert.assertFalse(logFile.exists());
        enableLog();
        if (logFile.exists()) {
            logFile.delete();
        }
        makeOneLog();
        // test that log-read-only=false works
        Assert.assertFalse(logFile.exists());
        enableLogReadOnly();
        logFile.delete();
        makeOneLog();
        Assert.assertTrue(logFile.exists());
        disableLog();
        if (logFile.exists()) {
            logFile.delete();
        }
        makeOneLog();
        Assert.assertFalse(logFile.exists());
    }

    private void makeOneLog() throws Exception {
        ObjectName objectName;
        try {
            objectName = ObjectName.getInstance("java.lang:*");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        connection.queryNames(objectName, null);
    }

    private void enableLog() throws Exception {
        ModelNode op = new ModelNode();
        op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(SUBSYSTEM, JMX);
        op.get(OP_ADDR).add(CONFIGURATION, AUDIT_LOG);
        op.get(NAME).set(ENABLED);
        op.get(VALUE).set("true");
        JmxAuditLogSetup.applyUpdate(managementClient.getControllerClient(), op, false);
    }

    private void disableLog() throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(SUBSYSTEM, JMX);
        op.get(OP_ADDR).add(CONFIGURATION, AUDIT_LOG);
        op.get(NAME).set(ENABLED);
        op.get(VALUE).set("false");
        JmxAuditLogSetup.applyUpdate(managementClient.getControllerClient(), op, false);
    }

    private void enableLogReadOnly() throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(SUBSYSTEM, JMX);
        op.get(OP_ADDR).add(CONFIGURATION, AUDIT_LOG);
        op.get(NAME).set(LOG_READ_ONLY);
        op.get(VALUE).set("true");
        JmxAuditLogSetup.applyUpdate(managementClient.getControllerClient(), op, false);
    }

    private MBeanServerConnection setupAndGetConnection() throws Exception {
        String urlString = System.getProperty("jmx.service.url",
                "service:jmx:http-remoting-jmx://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort());
        JMXServiceURL serviceURL = new JMXServiceURL(urlString);
        connector = JMXConnectorFactory.connect(serviceURL, null);
        return connector.getMBeanServerConnection();
    }

    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        return war;
    }

    static class JmxAuditLogSetup implements ServerSetupTask {

        private static final String HANDLER_NAME = "file";

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            logFile = new File(System.getProperty("jboss.home"));
            logFile = new File(logFile, "standalone");
            logFile = new File(logFile, "data");
            logFile = new File(logFile, "audit-log.log");
            if (logFile.exists()) {
                logFile.delete();
            }

            ModelNode op;
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, JMX);
            op.get(OP_ADDR).add(CONFIGURATION, AUDIT_LOG);
            op.get(ENABLED).set("false");
            op.get(LOG_READ_ONLY).set("false");
            applyUpdate(managementClient.getControllerClient(), op, false);

            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, JMX);
            op.get(OP_ADDR).add(CONFIGURATION, AUDIT_LOG);
            op.get(OP_ADDR).add(HANDLER, HANDLER_NAME);
            applyUpdate(managementClient.getControllerClient(), op, false);

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode op;

            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, JMX);
            op.get(OP_ADDR).add(CONFIGURATION, AUDIT_LOG);
            op.get(OP_ADDR).add(HANDLER, HANDLER_NAME);
            applyUpdate(managementClient.getControllerClient(), op, false);

            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, JMX);
            op.get(OP_ADDR).add(CONFIGURATION, AUDIT_LOG);
            applyUpdate(managementClient.getControllerClient(), op, false);

            if (logFile.exists()) {
                logFile.delete();
            }

        }

        static void applyUpdate(final ModelControllerClient client, ModelNode update, boolean allowFailure) throws Exception {
            ModelNode result = client.execute(new OperationBuilder(update).build());
            if (result.hasDefined("outcome") && (allowFailure || "success".equals(result.get("outcome").asString()))) {
                if (result.hasDefined("result")) {
                    System.out.println(result.get("result"));
                }
            } else if (result.hasDefined("failure-description")) {
                throw new RuntimeException(result.get("failure-description").toString());
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
            }
        }

    }
}
