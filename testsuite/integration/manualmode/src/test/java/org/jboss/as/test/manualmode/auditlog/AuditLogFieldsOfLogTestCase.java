package org.jboss.as.test.manualmode.auditlog;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_FORMAT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UDP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.audit.AccessAuditResourceDefinition;
import org.jboss.as.domain.management.audit.AuditLogLoggerResourceDefinition;
import org.jboss.as.test.integration.security.common.BlockedFileSyslogServerEventHandler;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.xnio.IoUtils;

/**
 * @author: Ondrej Lukas
 *
 *          Test that fields of Audit log have right content
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AuditLogFieldsOfLogTestCase {

    public static final String CONTAINER = "default-jbossas";
    private static final String FORMATTER = "formatter";
    private static final String JSON_FORMATTER = "json-formatter";
    private static final String SYSLOG_HANDLER_NAME = "audit-test-syslog-handler";
    final int PORT = 9276;

    @ArquillianResource
    private ContainerController container;

    ManagementClient managementClient;
    private File file;
    private File syslogFile;
    private PathAddress auditLogConfigAddress;
    private PathAddress mgmtRealmConfigAddress;
    private PathAddress syslogHandlerAddress;
    private PathAddress addSyslogHandler;
    private static SyslogServerIF server;
    private static BlockingQueue<String> queue;
    private static final int ADJUSTED_SECOND = TimeoutUtil.adjust(1000);

    @Test
    public void testAuditLoggingFields() throws Exception {
        container.start(CONTAINER);
        if (file.exists()) {
            file.delete();
        }
        Assert.assertTrue(makeOneLog());

        List<ModelNode> logs = readFile(file, 1);
        ModelNode log = logs.get(0);
        Assert.assertEquals("core", log.get("type").asString());
        Assert.assertEquals("false", log.get("r/o").asString());
        Assert.assertEquals("false", log.get("booting").asString());
        Assert.assertTrue(log.get("version").isDefined());
        Assert.assertEquals("IAmAdmin", log.get("user").asString());
        Assert.assertFalse(log.get("domainUUID").isDefined());
        Assert.assertEquals("NATIVE", log.get("access").asString());
        Assert.assertTrue(log.get("remote-address").isDefined());
        Assert.assertEquals("true", log.get("success").asString());
        List<ModelNode> operations = log.get("ops").asList();
        Assert.assertEquals(1, operations.size());

        if (syslogFile.exists()) {
            syslogFile.delete();
            server.getConfig().removeAllEventHandlers();
            server.getConfig().addEventHandler(
                    new BlockedFileSyslogServerEventHandler(queue, syslogFile.getAbsolutePath(), false));
        }
        Assert.assertTrue(makeOneLog());
        queue.poll(15 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
        List<ModelNode> syslogLogs = readFile(syslogFile, 1);
        ModelNode syslogLog = syslogLogs.get(0);
        Assert.assertEquals("core", syslogLog.get("type").asString());
        Assert.assertEquals("false", syslogLog.get("r/o").asString());
        Assert.assertEquals("false", syslogLog.get("booting").asString());
        Assert.assertTrue(log.get("version").isDefined());
        Assert.assertEquals("IAmAdmin", syslogLog.get("user").asString());
        Assert.assertFalse(syslogLog.get("domainUUID").isDefined());
        Assert.assertEquals("NATIVE", syslogLog.get("access").asString());
        Assert.assertTrue(syslogLog.get("remote-address").isDefined());
        Assert.assertEquals("true", syslogLog.get("success").asString());
        List<ModelNode> syslogOperations = syslogLog.get("ops").asList();
        Assert.assertEquals(1, syslogOperations.size());

    }

    private boolean makeOneLog() throws IOException {
        ModelNode op = Util.getWriteAttributeOperation(auditLogConfigAddress,
                AuditLogLoggerResourceDefinition.LOG_BOOT.getName(), new ModelNode(true));
        ModelNode result = managementClient.getControllerClient().execute(op);
        return SUCCESS.equals(result.get(OUTCOME).asString());
    }

    @Before
    public void beforeTest() throws Exception {
        file = new File(System.getProperty("jboss.home"));
        file = new File(file, "standalone");
        file = new File(file, "data");
        file = new File(file, "audit-log.log");
        if (file.exists()) {
            file.delete();
        }

        syslogFile = new File(System.getProperty("jboss.home"));
        syslogFile = new File(syslogFile, "standalone");
        syslogFile = new File(syslogFile, "data");
        syslogFile = new File(syslogFile, "syslog-audit-log.log");
        if (syslogFile.exists()) {
            syslogFile.delete();
        }

        // start and set syslog server
        server = SyslogServer.getInstance("udp");
        server.getConfig().setPort(PORT);
        queue = new LinkedBlockingQueue<String>();
        server.getConfig().addEventHandler(new BlockedFileSyslogServerEventHandler(queue, syslogFile.getAbsolutePath(), false));
        SyslogServer.getThreadedInstance("udp");

        // Start the server
        container.start(CONTAINER);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(),
                TestSuiteEnvironment.getServerPort(), "http-remoting");

        ModelNode op;
        ModelNode result;

        mgmtRealmConfigAddress = PathAddress.pathAddress(PathElement.pathElement(CORE_SERVICE, MANAGEMENT),
                PathElement.pathElement(SECURITY_REALM, "ManagementRealm"), PathElement.pathElement(AUTHENTICATION, LOCAL));
        op = Util.getWriteAttributeOperation(mgmtRealmConfigAddress, "default-user", new ModelNode("IAmAdmin"));
        result = client.execute(op);

        auditLogConfigAddress = PathAddress.pathAddress(CoreManagementResourceDefinition.PATH_ELEMENT,
                AccessAuditResourceDefinition.PATH_ELEMENT, AuditLogLoggerResourceDefinition.PATH_ELEMENT);

        op = Util.getWriteAttributeOperation(auditLogConfigAddress, AuditLogLoggerResourceDefinition.ENABLED.getName(),
                new ModelNode(true));
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(), SUCCESS, result.get(OUTCOME).asString());

        ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();
        ModelNode steps = compositeOp.get(STEPS);
        syslogHandlerAddress = PathAddress.pathAddress(PathElement.pathElement(CORE_SERVICE, MANAGEMENT),
                PathElement.pathElement(ACCESS, AUDIT), PathElement.pathElement(SYSLOG_HANDLER, SYSLOG_HANDLER_NAME));
        op = Util.createAddOperation(syslogHandlerAddress);
        op.get(FORMATTER).set(JSON_FORMATTER);
        op.get(SYSLOG_FORMAT).set("RFC5424");
        steps.add(op);
        op = new ModelNode();
        PathAddress syslogProtocol = PathAddress.pathAddress(syslogHandlerAddress, PathElement.pathElement(PROTOCOL, UDP));
        op = Util.createAddOperation(syslogProtocol);
        op.get("port").set(PORT);
        op.get("host").set("localhost");
        steps.add(op);
        result = client.execute(compositeOp);
        Assert.assertEquals(result.get("failure-description").asString(), SUCCESS, result.get(OUTCOME).asString());

        addSyslogHandler = PathAddress.pathAddress(auditLogConfigAddress, PathElement.pathElement(HANDLER, SYSLOG_HANDLER_NAME));
        op = Util.createAddOperation(addSyslogHandler);
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(), SUCCESS, result.get(OUTCOME).asString());

        container.stop(CONTAINER);
        Thread.sleep(1000);
        while (managementClient.isServerInRunningState()) {
            Thread.sleep(50);
        }
    }

    @After
    public void afterTest() throws Exception {
        // stop syslog server
        SyslogServer.shutdown();

        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        ModelNode op = Util.getWriteAttributeOperation(auditLogConfigAddress, AuditLogLoggerResourceDefinition.ENABLED.getName(),
                new ModelNode(false));
        client.execute(op);
        op = Util.getWriteAttributeOperation(mgmtRealmConfigAddress, "default-user", new ModelNode("$local"));
        client.execute(op);

        op = Util.getResourceRemoveOperation(addSyslogHandler);
        client.execute(op);
        op = Util.getResourceRemoveOperation(syslogHandlerAddress);
        client.execute(op);

        if (file.exists()) {
            file.delete();
        }
        if (syslogFile.exists()) {
            syslogFile.delete();
        }
        try {
            // Stop the container
            container.stop(CONTAINER);
        } finally {
            IoUtils.safeClose(client);
        }
    }

    private final Pattern DATE_STAMP_PATTERN = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d - \\{");

    protected List<ModelNode> readFile(File file, int expectedRecords) throws IOException {
        List<ModelNode> list = new ArrayList<ModelNode>();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            StringWriter writer = null;
            String line = reader.readLine();
            while (line != null) {
                if (DATE_STAMP_PATTERN.matcher(line).find()) {
                    if (writer != null) {
                        list.add(ModelNode.fromJSONString(writer.getBuffer().toString()));
                    }
                    writer = new StringWriter();
                    writer.append("{");
                } else {
                    if (writer != null)
                        writer.append("\n" + line);
                }
                line = reader.readLine();
            }
            if (writer != null) {
                list.add(ModelNode.fromJSONString(writer.getBuffer().toString()));
            }
        } finally {
            IoUtils.safeClose(reader);
        }
        Assert.assertEquals(list.toString(), expectedRecords, list.size());
        return list;
    }

}
