package org.jboss.as.test.manualmode.auditlog;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUDIT_LOG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FORMATTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JSON_FORMATTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.File;

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
 * Test that attribute log-boot of audit-log in Management and audit-log in JMX works right
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AuditLogBootingLogTestCase {
	
	public static final String CONTAINER = "default-jbossas";
	private static final String JMX = "jmx";
    private static final String CONFIGURATION = "configuration";    
    private static final String HANDLER_NAME = "file2";
	
	@ArquillianResource
    private ContainerController container;

    ManagementClient managementClient;    
    private File auditLogFile;
    private PathAddress auditLogConfigAddress;
    private File jmxLogFile;
    private PathAddress jmxLogConfigAddress;
    private PathAddress jmxFormatterConfigAddress;
    
    @Test
    public void testBootIsLogged() throws Exception {
    	if (auditLogFile.exists()){
            auditLogFile.delete();
        }
    	if (jmxLogFile.exists()){
            jmxLogFile.delete();
        }
        container.start(CONTAINER);
    	Assert.assertTrue("Booting logs weren't logged but log-boot is set to true", auditLogFile.exists());
    	Assert.assertTrue("Booting jmx logs weren't logged but log-boot is set to true", jmxLogFile.exists());
    	
    	beforeTestBootIsNotLogged();
    	
    	container.stop(CONTAINER);
	    Thread.sleep(1000);
	    while (managementClient.isServerInRunningState()) {
	        Thread.sleep(50);
	    }
	    
	    if (auditLogFile.exists()){
	        auditLogFile.delete();
	    }
	  	if (jmxLogFile.exists()){
	        jmxLogFile.delete();
	    }

	    container.start(CONTAINER);
	  	Assert.assertFalse("Booting logs were logged but log-boot is set to false", auditLogFile.exists());
	  	Assert.assertFalse("Booting jmx logs were logged but log-boot is set to false", jmxLogFile.exists());
    }  
    
    private void beforeTestBootIsNotLogged() throws Exception {
    	final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
    	ModelNode op;
    	ModelNode result;
    	op = Util.getWriteAttributeOperation(
                auditLogConfigAddress,
                AuditLogLoggerResourceDefinition.LOG_BOOT.getName(),
                new ModelNode(false));
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
        
        op = Util.getWriteAttributeOperation(
        		jmxLogConfigAddress,
                AuditLogLoggerResourceDefinition.LOG_BOOT.getName(),
                new ModelNode(false));
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
    }

    @Before
    public void beforeTest() throws Exception {
    	auditLogFile = new File(System.getProperty("jboss.home"));
        auditLogFile = new File(auditLogFile, "standalone");
        auditLogFile = new File(auditLogFile, "data");
        auditLogFile = new File(auditLogFile, "audit-log.log");
        if (auditLogFile.exists()){
            auditLogFile.delete();
        }
        
        jmxLogFile = new File(System.getProperty("jboss.home"));
        jmxLogFile = new File(jmxLogFile, "standalone");
        jmxLogFile = new File(jmxLogFile, "data");
        jmxLogFile = new File(jmxLogFile, "jmx-log.log");
        if (jmxLogFile.exists()){
        	jmxLogFile.delete();
        }
    	
        // Start the server
        container.start(CONTAINER);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
        
        ModelNode op;
        ModelNode result;
        auditLogConfigAddress = PathAddress.pathAddress(
                CoreManagementResourceDefinition.PATH_ELEMENT,
                AccessAuditResourceDefinition.PATH_ELEMENT,
                AuditLogLoggerResourceDefinition.PATH_ELEMENT);

        //Enable audit logging and boot operations
        op = Util.getWriteAttributeOperation(
                auditLogConfigAddress,
                AuditLogLoggerResourceDefinition.LOG_BOOT.getName(),
                new ModelNode(true));
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
        
        op = Util.getWriteAttributeOperation(
                auditLogConfigAddress,
                AuditLogLoggerResourceDefinition.ENABLED.getName(),
                new ModelNode(true));
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
        
        jmxFormatterConfigAddress = PathAddress.pathAddress(
        		PathElement.pathElement(CORE_SERVICE, MANAGEMENT),
        		PathElement.pathElement(ACCESS, AUDIT),
        		PathElement.pathElement(FILE_HANDLER, HANDLER_NAME));
        op = Util.createAddOperation(jmxFormatterConfigAddress);
        op.get(FORMATTER).set(JSON_FORMATTER);
        op.get(PATH).set("jmx-log.log");
        op.get(RELATIVE_TO).set("jboss.server.data.dir");
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());        
        
        jmxLogConfigAddress = PathAddress.pathAddress(
        		PathElement.pathElement(SUBSYSTEM, JMX),
        		PathElement.pathElement(CONFIGURATION, AUDIT_LOG));
        
        op = Util.createAddOperation(jmxLogConfigAddress);
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
        
        op = Util.createAddOperation(PathAddress.pathAddress(jmxLogConfigAddress,
        		PathElement.pathElement(HANDLER, HANDLER_NAME)));
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
        
        op = Util.getWriteAttributeOperation(
        		jmxLogConfigAddress,
                AuditLogLoggerResourceDefinition.LOG_BOOT.getName(),
                new ModelNode(true));
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString());
        
        op = Util.getWriteAttributeOperation(
        		jmxLogConfigAddress,
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
    	ModelNode result;
    	ModelNode op = Util.getWriteAttributeOperation(
                auditLogConfigAddress,
                AuditLogLoggerResourceDefinition.ENABLED.getName(),
                new ModelNode(false));
        result = client.execute(op);   
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString()); 
        
        op = Util.getResourceRemoveOperation(PathAddress.pathAddress(jmxLogConfigAddress,
        		PathElement.pathElement(HANDLER, HANDLER_NAME)));
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString()); 
        
        op = Util.getResourceRemoveOperation(jmxLogConfigAddress);
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString()); 
        
        op = Util.getResourceRemoveOperation(jmxFormatterConfigAddress);
        result = client.execute(op);
        Assert.assertEquals(result.get("failure-description").asString(),SUCCESS, result.get(OUTCOME).asString()); 
        
        if (auditLogFile.exists()) {
        	auditLogFile.delete();
        }
        if (jmxLogFile.exists()) {
        	jmxLogFile.delete();
        }
        try {
            // Stop the container
            container.stop(CONTAINER);
        } finally {
            IoUtils.safeClose(client);
        }
    }

}
