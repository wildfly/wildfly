package org.jboss.as.test.manualmode.auditlog;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHENTICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT_LOG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HANDLER;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.management.audit.AuditLogLoggerResourceDefinition;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 * @author: Ondrej Lukas
 * 
 * Test that fields of Audit log from JMX have right content
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JmxAuditLogFieldsOfLogTestCase {
	
	public static final String CONTAINER = "default-jbossas";
	
	@ArquillianResource
    private ContainerController container;

    ManagementClient managementClient;    
    private File file;
    private PathAddress auditLogConfigAddress;
    private PathAddress mgmtRealmConfigAddress;
    
    static JMXConnector connector;
    static MBeanServerConnection connection;
    
    private static final String JMX = "jmx";
    private static final String CONFIGURATION = "configuration";    
    private static final String HANDLER_NAME = "file";
    
    @Test
    public void testJmxAuditLoggingFields() throws Exception {
    	container.start(CONTAINER);
    	connection = setupAndGetConnection();
    	if (file.exists()){
            file.delete();
        }
    	makeOneLog();
    	Assert.assertTrue(file.exists());
    	List<ModelNode> logs = readFile(file,1);
    	ModelNode log = logs.get(0);
        Assert.assertEquals("jmx", log.get("type").asString());
        Assert.assertEquals("true", log.get("r/o").asString());
        Assert.assertEquals("false", log.get("booting").asString());
        Assert.assertTrue(log.get("version").isDefined());
        Assert.assertEquals("IAmAdmin", log.get("user").asString());
        Assert.assertFalse(log.get("domainUUID").isDefined());
        Assert.assertEquals("JMX",log.get("access").asString());
        Assert.assertTrue(log.get("remote-address").isDefined());
        Assert.assertEquals("queryMBeans", log.get("method").asString());
        List<ModelNode> sig = log.get("sig").asList();
        Assert.assertEquals(2, sig.size());
        List<ModelNode> params = log.get("params").asList();
        Assert.assertEquals(2, params.size());
    }

    private void makeOneLog() throws IOException {
    	ObjectName objectName;
        try {
            objectName = ObjectName.getInstance("java.lang:*");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        connection.queryNames(objectName, null);
    }
    
    @Before
    public void beforeTest() throws Exception {
    	file = new File(System.getProperty("jboss.home"));
        file = new File(file, "standalone");
        file = new File(file, "data");
        file = new File(file, "audit-log.log");
        if (file.exists()){
            file.delete();
        }        
        
        // Start the server
        container.start(CONTAINER);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        
        ModelNode op;
        ModelNode result;
        
        mgmtRealmConfigAddress = PathAddress.pathAddress(
        		PathElement.pathElement(CORE_SERVICE, MANAGEMENT),
        		PathElement.pathElement(SECURITY_REALM, "ManagementRealm"),
        		PathElement.pathElement(AUTHENTICATION, LOCAL));
        op = Util.getWriteAttributeOperation(mgmtRealmConfigAddress,
                "default-user",
                new ModelNode("IAmAdmin"));        
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
        
        auditLogConfigAddress = PathAddress.pathAddress(
        		PathElement.pathElement(SUBSYSTEM, JMX),
        		PathElement.pathElement(CONFIGURATION, AUDIT_LOG));
        
        op = Util.createAddOperation(auditLogConfigAddress);
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
        
        op = Util.createAddOperation(PathAddress.pathAddress(auditLogConfigAddress,
        		PathElement.pathElement(HANDLER, HANDLER_NAME)));
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
        
        op = Util.getWriteAttributeOperation(
                auditLogConfigAddress,
                AuditLogLoggerResourceDefinition.LOG_READ_ONLY.getName(),
                new ModelNode(true));
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
        
        op = Util.getWriteAttributeOperation(
                auditLogConfigAddress,
                AuditLogLoggerResourceDefinition.ENABLED.getName(),
                new ModelNode(true));
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
        container.stop(CONTAINER);
        Thread.sleep(1000);
        while (managementClient.isServerInRunningState()) {
            Thread.sleep(50);
        }
    }
    
    @After
    public void afterTest() throws Exception {
    	final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
    	ModelNode op = Util.getWriteAttributeOperation(mgmtRealmConfigAddress,
                "default-user",
                new ModelNode("$local"));        
        client.execute(op);
        op = Util.getResourceRemoveOperation(PathAddress.pathAddress(auditLogConfigAddress,
        		PathElement.pathElement(HANDLER, HANDLER_NAME)));
        client.execute(op);
        op = Util.getResourceRemoveOperation(auditLogConfigAddress);
        client.execute(op);
        if (file.exists()) {
        	file.delete();
        }
        IoUtils.safeClose(connector);
        try {
            // Stop the container
            container.stop(CONTAINER);
        } finally {
            IoUtils.safeClose(client);
        }
    }
    
    private MBeanServerConnection setupAndGetConnection() throws Exception {
        String urlString = System.getProperty("jmx.service.url",
                "service:jmx:http-remoting-jmx://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort());
        JMXServiceURL serviceURL = new JMXServiceURL(urlString);
        connector = JMXConnectorFactory.connect(serviceURL, null);
        return connector.getMBeanServerConnection();
    }
    
    private final Pattern DATE_STAMP_PATTERN = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d - \\{");

    protected List<ModelNode> readFile(File file, int expectedRecords) throws IOException {
        List<ModelNode> list = new ArrayList<ModelNode>();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            StringWriter writer = null;
            String line = reader.readLine();
            while (line != null) {
                if (DATE_STAMP_PATTERN.matcher(line).matches()) {
                    if (writer != null) {
                        list.add(ModelNode.fromJSONString(writer.getBuffer().toString()));
                    }
                    writer = new StringWriter();
                    writer.append("{");
                } else {
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
