package org.jboss.as.test.manualmode.adminonly.auditlog;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.File;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.audit.AccessAuditResourceDefinition;
import org.jboss.as.domain.management.audit.AuditLogLoggerResourceDefinition;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 * @author: Kabir Khan
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AdminOnlyAuditLogTestCase {
    public static final String CONTAINER = "jbossas-admin-only";

    @ArquillianResource
    private ContainerController container;

    ManagementClient managementClient;

    @Before
    public void startContainer() throws Exception {
        // Start the server
        container.start(CONTAINER);
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "http-remoting");
    }

    @After
    public void stopContainer() throws Exception {
        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient();
        try {
            // Stop the container
            container.stop(CONTAINER);
        } finally {
            IoUtils.safeClose(client);
        }
    }

    @Test
    public void testEnableAndDisableCoreAuditLog() throws Exception {
        File file = new File(System.getProperty("jboss.home"));
        file = new File(file, "standalone");
        file = new File(file, "data");
        file = new File(file, "audit-log.log");
        if (file.exists()){
            file.delete();
        }

        System.out.println(file.getAbsolutePath());

        ModelControllerClient client = managementClient.getControllerClient();
        try {
            ModelNode op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
            ModelNode result = client.execute(op);
            junit.framework.Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
            junit.framework.Assert.assertFalse(file.exists());

            PathAddress auditLogConfigAddress = PathAddress.pathAddress(
                    CoreManagementResourceDefinition.PATH_ELEMENT,
                    AccessAuditResourceDefinition.PATH_ELEMENT,
                    AuditLogLoggerResourceDefinition.PATH_ELEMENT);

            //Enable audit logging
            op = Util.getWriteAttributeOperation(
                    auditLogConfigAddress,
                    AuditLogLoggerResourceDefinition.ENABLED.getName(),
                    new ModelNode(true));
            result = client.execute(op);
            junit.framework.Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
            junit.framework.Assert.assertTrue(file.exists());

            try {
                file.delete();
                op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
                result = client.execute(op);
                junit.framework.Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
                junit.framework.Assert.assertTrue(file.exists());

            } finally {
                file.delete();
                //Disable audit logging again
                op = Util.getWriteAttributeOperation(
                        auditLogConfigAddress,
                        AuditLogLoggerResourceDefinition.ENABLED.getName(),
                        new ModelNode(false));
                result = client.execute(op);
                junit.framework.Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
                junit.framework.Assert.assertTrue(file.exists());

                file.delete();
                op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
                result = client.execute(op);
                junit.framework.Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
                junit.framework.Assert.assertFalse(file.exists());
            }
        } finally {
            IoUtils.safeClose(client);
        }
    }
}
