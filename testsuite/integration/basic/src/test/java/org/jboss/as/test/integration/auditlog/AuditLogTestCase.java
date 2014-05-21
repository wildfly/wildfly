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
 *
 */
package org.jboss.as.test.integration.auditlog;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CODE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CUSTOM_FORMATTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FORMATTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.audit.AccessAuditResourceDefinition;
import org.jboss.as.domain.management.audit.AuditLogLoggerResourceDefinition;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.test.integration.auditlog.module.custom.TestCustomAuditLogEventFormatterFactory;
import org.jboss.as.test.module.util.TestModule;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 * The actual contents of what is being logged is tested in
 * org.jboss.as.domain.management.security.auditlog.AuditLogHandlerTestCase.
 * This test does some simple checks to make sure that audit logging kicks in in a
 * standalone instance.
 *
 * @author: Kabir Khan
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AuditLogTestCase {

    private static String MODULE_NAME = "test.custom.audit.log.formatter";
    private static String JAR_NAME = "custom-custom-audit-log-formatter-module.jar";
    private static final File WORK_DIR = new File("custom-audit-log-formatter-workdir");
    private static final File MODULE_FILE = new File(WORK_DIR, "module.xml");
    private static final PathAddress AUDIT_LOG_CONFIG_ADDRESS = PathAddress.pathAddress(
            CoreManagementResourceDefinition.PATH_ELEMENT,
            AccessAuditResourceDefinition.PATH_ELEMENT,
            AuditLogLoggerResourceDefinition.PATH_ELEMENT);


    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testEnableAndDisableCoreAuditLog() throws Exception {
        File file = getFile();

        ModelControllerClient client = managementClient.getControllerClient();
        ModelNode op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
        ModelNode result = client.execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertFalse(file.exists());


        //Enable audit logging and read only operations
        op = Util.getWriteAttributeOperation(
                AUDIT_LOG_CONFIG_ADDRESS,
                AuditLogLoggerResourceDefinition.LOG_READ_ONLY.getName(),
                new ModelNode(true));
        result = client.execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        op = Util.getWriteAttributeOperation(
                AUDIT_LOG_CONFIG_ADDRESS,
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
                    AUDIT_LOG_CONFIG_ADDRESS,
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
        }
    }

    @Test
    public void testCustomAuditLogFormatter() throws Exception {
        final File file = getFile();
        final PathAddress customFormatterAddress = PathAddress.pathAddress(
                CoreManagementResourceDefinition.PATH_ELEMENT,
                AccessAuditResourceDefinition.PATH_ELEMENT,
                PathElement.pathElement(CUSTOM_FORMATTER, "test-custom"));

        final PathAddress fileHandlerAddress = PathAddress.pathAddress(
                CoreManagementResourceDefinition.PATH_ELEMENT,
                AccessAuditResourceDefinition.PATH_ELEMENT,
                PathElement.pathElement(FILE_HANDLER, "file"));

        String moduleXML = "<module xmlns=\"urn:jboss:module:1.1\" name=\""+ MODULE_NAME +"\">" +
                "<resources> <resource-root path=\""+ JAR_NAME +"\"/>  </resources>" +
                "<dependencies> <module name=\"org.jboss.as.controller\"/> </dependencies> "+
            "</module>";

        FileUtils.write(MODULE_FILE, moduleXML);
        TestModule customFormatterModule = new TestModule(MODULE_NAME, MODULE_FILE);
        customFormatterModule.addResource(JAR_NAME).addPackage(TestCustomAuditLogEventFormatterFactory.class.getPackage());
        customFormatterModule.create(true);

        try {
            //Add custom formatter
            ModelControllerClient client = managementClient.getControllerClient();
            ModelNode op = Util.createAddOperation(customFormatterAddress);
            op.get(MODULE).set(MODULE_NAME);
            op.get(CODE).set(TestCustomAuditLogEventFormatterFactory.class.getName());

            ModelTestUtils.checkOutcome(client.execute(op));

            try {
                //Add a formatter property
                op = Util.createAddOperation(customFormatterAddress.append(PROPERTY, "A"));
                op.get(VALUE).set("1");
                ModelTestUtils.checkOutcome(client.execute(op));
                Assert.assertFalse(file.exists());

                //Update to use the audit logger
                op = Util.getWriteAttributeOperation(fileHandlerAddress, FORMATTER, new ModelNode("test-custom"));
                ModelTestUtils.checkOutcome(client.execute(op));
                try {
                    Assert.assertFalse(file.exists());
                    //Enable audit logging
                    op = Util.getWriteAttributeOperation(
                            AUDIT_LOG_CONFIG_ADDRESS,
                            AuditLogLoggerResourceDefinition.ENABLED.getName(),
                            new ModelNode(true));
                    ModelTestUtils.checkOutcome(client.execute(op));
                    try {
                        //Check the custom audit log record
                        Assert.assertTrue(file.exists());
                        String contents = readFullFileRecord(file);
                        Assert.assertEquals("[A=1]" + ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, contents);
                    } finally {
                        op = Util.getWriteAttributeOperation(
                                AUDIT_LOG_CONFIG_ADDRESS,
                                AuditLogLoggerResourceDefinition.ENABLED.getName(),
                                new ModelNode(false));
                    }
                } finally {
                    op = Util.getWriteAttributeOperation(fileHandlerAddress, FORMATTER, new ModelNode("json-formatter"));
                    ModelTestUtils.checkOutcome(client.execute(op));
                }

            } finally {
                client.execute(Util.createRemoveOperation(customFormatterAddress));
            }

        } finally {
            customFormatterModule.remove();
        }


    }

    private File getFile() {
        File file = new File(System.getProperty("jboss.home"));
        file = new File(file, "standalone");
        file = new File(file, "data");
        file = new File(file, "audit-log.log");
        if (file.exists()){
            Assert.assertTrue(file.delete());
        }
        return file;
    }

    protected String readFullFileRecord(File file) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            boolean firstLine = true;
            StringWriter writer = new StringWriter();
            String line = reader.readLine();
            while (line != null) {
                if (!firstLine) {
                    writer.append("\n");
                } else {
                    firstLine = false;
                }
                writer.append(line);
                line = reader.readLine();
            }
            return writer.toString();
        } finally {
            IoUtils.safeClose(reader);
        }
    }
}
