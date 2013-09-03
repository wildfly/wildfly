package org.jboss.as.test.integration.auditlog;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT_LOG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOGGER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOG_BOOT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOG_READ_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_FORMAT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UDP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.security.common.BlockedFileSyslogServerEventHandler;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.xnio.IoUtils;

/**
 * Test that syslog-handler logs in Audit Log
 * 
 * @author: Ondrej Lukas
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(AuditLogToSyslogTestCase.AuditLogToSyslogSetup.class)
public class AuditLogToSyslogTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    private static final String FILE_NAME = "tempSyslogFile.log";
    private static File logFile;
    private static SyslogServerIF server;
    private static BlockingQueue<String> queue;
    private static final int ADJUSTED_SECOND = TimeoutUtil.adjust(1000);

    @Test
    public void testAuditLoggingToSyslog() throws Exception {
        if (logFile.exists()) {
            logFile.delete();
            server.getConfig().removeAllEventHandlers();
            server.getConfig()
                    .addEventHandler(new BlockedFileSyslogServerEventHandler(queue, logFile.getAbsolutePath(), false));
        }
        Assert.assertEquals(0, readFile(logFile));
        makeOneLog();
        queue.poll(3 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Audit log is logged to syslog even it isn't enabled", 0, readFile(logFile));
        try {
            enableLog();
            Assert.assertEquals(1, readFile(logFile));
            makeOneLog();
            queue.poll(15 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
            Assert.assertEquals("Audit log isn't logged to syslog", 2, readFile(logFile));
        } finally {
            disableLog();
        }
        if (logFile.exists()) {
            logFile.delete();
            server.getConfig().removeAllEventHandlers();
            server.getConfig()
                    .addEventHandler(new BlockedFileSyslogServerEventHandler(queue, logFile.getAbsolutePath(), false));
        }
        makeOneLog();
        queue.poll(3 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals("Audit log is logged to syslog even it was disabled", 0, readFile(logFile));
    }

    private void enableLog() throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
        op.get(OP_ADDR).add(ACCESS, AUDIT);
        op.get(OP_ADDR).add(LOGGER, AUDIT_LOG);
        op.get(NAME).set(ENABLED);
        op.get(VALUE).set("true");
        AuditLogToSyslogSetup.applyUpdate(managementClient.getControllerClient(), op, false);
    }

    private void disableLog() throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
        op.get(OP_ADDR).add(ACCESS, AUDIT);
        op.get(OP_ADDR).add(LOGGER, AUDIT_LOG);
        op.get(NAME).set(ENABLED);
        op.get(VALUE).set("false");
        AuditLogToSyslogSetup.applyUpdate(managementClient.getControllerClient(), op, false);
    }

    private void makeOneLog() throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
        op.get(OP_ADDR).add(ACCESS, AUDIT);
        op.get(OP_ADDR).add(LOGGER, AUDIT_LOG);
        op.get(NAME).set(LOG_BOOT);
        op.get(VALUE).set("true");
        AuditLogToSyslogSetup.applyUpdate(managementClient.getControllerClient(), op, false);
    }

    private final Pattern DATE_STAMP_PATTERN = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d - \\{");

    protected int readFile(File file) throws IOException {
        int counter = 0;
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            String line = reader.readLine();
            while (line != null) {
                if (DATE_STAMP_PATTERN.matcher(line).find()) {
                    counter++;
                }
                line = reader.readLine();
            }
        } finally {
            IoUtils.safeClose(reader);
        }
        return counter;
    }

    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        return war;
    }

    static class AuditLogToSyslogSetup implements ServerSetupTask {

        private static final String FORMATTER = "formatter";
        private static final String JSON_FORMATTER = "json-formatter";
        private static final String SYSLOG_HANDLER_NAME = "audit-test-syslog-handler";

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            final int PORT = 9176;

            logFile = new File(System.getProperty("java.io.tmpdir"), FILE_NAME);

            if (logFile.exists()) {
                logFile.delete();
            }

            // start and set syslog server
            server = SyslogServer.getInstance("udp");
            server.getConfig().setPort(PORT);
            queue = new LinkedBlockingQueue<String>();
            server.getConfig()
                    .addEventHandler(new BlockedFileSyslogServerEventHandler(queue, logFile.getAbsolutePath(), false));
            SyslogServer.getThreadedInstance("udp");

            ModelNode op;

            // nastavit logovani do syslog
            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            ModelNode steps = compositeOp.get(STEPS);
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(ACCESS, AUDIT);
            op.get(OP_ADDR).add(SYSLOG_HANDLER, SYSLOG_HANDLER_NAME);
            op.get(FORMATTER).set(JSON_FORMATTER);
            op.get(SYSLOG_FORMAT).set("RFC5424");
            steps.add(op);
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(ACCESS, AUDIT);
            op.get(OP_ADDR).add(SYSLOG_HANDLER, SYSLOG_HANDLER_NAME);
            op.get(OP_ADDR).add(PROTOCOL, UDP);
            op.get("port").set(PORT);
            op.get("host").set("localhost");
            steps.add(op);
            applyUpdate(managementClient.getControllerClient(), compositeOp, false);

            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(ACCESS, AUDIT);
            op.get(OP_ADDR).add(LOGGER, AUDIT_LOG);
            op.get(OP_ADDR).add(HANDLER, SYSLOG_HANDLER_NAME);
            applyUpdate(managementClient.getControllerClient(), op, false);
            op = new ModelNode();
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(ACCESS, AUDIT);
            op.get(OP_ADDR).add(LOGGER, AUDIT_LOG);
            op.get(NAME).set(LOG_READ_ONLY);
            op.get(VALUE).set("false");
            applyUpdate(managementClient.getControllerClient(), op, false);

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // stop syslog server
            SyslogServer.shutdown();

            ModelNode op;

            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(ACCESS, AUDIT);
            op.get(OP_ADDR).add(LOGGER, AUDIT_LOG);
            op.get(OP_ADDR).add(HANDLER, SYSLOG_HANDLER_NAME);
            applyUpdate(managementClient.getControllerClient(), op, false);

            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(ACCESS, AUDIT);
            op.get(OP_ADDR).add(SYSLOG_HANDLER, SYSLOG_HANDLER_NAME);
            applyUpdate(managementClient.getControllerClient(), op, false);

            op = new ModelNode();
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(OP_ADDR).add(CORE_SERVICE, MANAGEMENT);
            op.get(OP_ADDR).add(ACCESS, AUDIT);
            op.get(OP_ADDR).add(LOGGER, AUDIT_LOG);
            op.get(NAME).set(LOG_READ_ONLY);
            op.get(VALUE).set("false");
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
