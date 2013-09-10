/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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
import org.junit.Assert;
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
        managementClient = new ManagementClient(client, TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort());
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

        ModelControllerClient client = managementClient.getControllerClient();
        try {
            ModelNode op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
            ModelNode result = client.execute(op);
            Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
            Assert.assertFalse(file.exists());

            PathAddress auditLogConfigAddress = PathAddress.pathAddress(
                    CoreManagementResourceDefinition.PATH_ELEMENT,
                    AccessAuditResourceDefinition.PATH_ELEMENT,
                    AuditLogLoggerResourceDefinition.PATH_ELEMENT);

            //Enable audit logging and read only operations
            op = Util.getWriteAttributeOperation(
                    auditLogConfigAddress,
                    AuditLogLoggerResourceDefinition.LOG_READ_ONLY.getName(),
                    new ModelNode(true));
            result = client.execute(op);
            Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
            op = Util.getWriteAttributeOperation(
                    auditLogConfigAddress,
                    AuditLogLoggerResourceDefinition.ENABLED.getName(),
                    new ModelNode(true));
            result = client.execute(op);
            Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
            Assert.assertTrue(file.exists());

            try {
                file.delete();
                op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
                result = client.execute(op);
                Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
                Assert.assertTrue(file.exists());

            } finally {
                file.delete();
                //Disable audit logging again
                op = Util.getWriteAttributeOperation(
                        auditLogConfigAddress,
                        AuditLogLoggerResourceDefinition.ENABLED.getName(),
                        new ModelNode(false));
                result = client.execute(op);
                Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
                Assert.assertTrue(file.exists());

                file.delete();
                op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
                result = client.execute(op);
                Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
                Assert.assertFalse(file.exists());

                //Set read-only operations back to false
                op = Util.getWriteAttributeOperation(
                        auditLogConfigAddress,
                        AuditLogLoggerResourceDefinition.LOG_READ_ONLY.getName(),
                        new ModelNode(false));
                result = client.execute(op);
                Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

            }
        } finally {
            IoUtils.safeClose(client);
        }
    }
}
