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
package org.jboss.as.test.integration.logging.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A SyslogHandlerAttributeTestCase for testing that attributes set via add operation and write-attribute operations are really
 * set
 * 
 * @author Ondrej Lukas
 */
@RunWith(Arquillian.class)
@ServerSetup(SyslogHandlerAttributesTestCase.SyslogHandlerTestCaseSetup.class)
@RunAsClient
public class SyslogHandlerAttributesTestCase {

    @ArquillianResource
    ManagementClient managementClient;

    private static final String SYSLOG_HANDLER_NAME = "SYSLOG";
    private static final String LEVEL = "level";
    private static final String PORT = "port";
    private static final String APP_NAME = "app-name";
    private static final String ENABLED = "enabled";
    private static final String FACILITY = "facility";
    private static final String SERVER_ADDRESS = "server-address";
    private static final String HOSTNAME = "hostname";
    private static final String SYSLOG_FORMAT = "syslog-format";
    private static final PathAddress syslogHandlerAddress = PathAddress.pathAddress(
            PathElement.pathElement(SUBSYSTEM, "logging"), PathElement.pathElement("syslog-handler", SYSLOG_HANDLER_NAME));

    @Test
    public void testSettingSyslogAttributes() throws Exception {
        checkAttribute("level", "TRACE");
        checkAttribute("port", "9876");
        checkAttribute("app-name", "old_name");
        checkAttribute("enabled", "false");
        checkAttribute("facility", "kernel");
        checkAttribute("server-address", "127.0.0.1");
        checkAttribute("hostname", "old_hostname");
        checkAttribute("syslog-format", "RFC5424");
        writeNewAttributes();
        checkAttribute("level", "INFO");
        checkAttribute("port", "9678");
        checkAttribute("app-name", "new_name");
        checkAttribute("enabled", "true");
        checkAttribute("facility", "user-level");
        checkAttribute("server-address", "127.0.0.2");
        checkAttribute("hostname", "new_hostname");
        checkAttribute("syslog-format", "RFC3164");
    }

    private void checkAttribute(String name, String expectedValue) throws IOException {
        ModelNode op;
        op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(SUBSYSTEM, "logging");
        op.get(OP_ADDR).add("syslog-handler", SYSLOG_HANDLER_NAME);
        op.get("name").set(name);
        String result = managementClient.getControllerClient().execute(op).get("result").toString();
        result = result.replaceAll("\"", "");
        assertTrue(name + " wasn't set right, expected: " + expectedValue + " but was: " + result, result.equals(expectedValue));
    }

    // write new attributes via write-attribute operation
    private void writeNewAttributes() throws IOException {
        writeSyslogHandlerAttributeOperation("port", new ModelNode(9678));
        writeSyslogHandlerAttributeOperation("app-name", new ModelNode("new_name"));
        writeSyslogHandlerAttributeOperation("enabled", new ModelNode("true"));
        writeSyslogHandlerAttributeOperation("level", new ModelNode("INFO"));
        writeSyslogHandlerAttributeOperation("facility", new ModelNode("user-level"));
        writeSyslogHandlerAttributeOperation("server-address", new ModelNode("127.0.0.2"));
        writeSyslogHandlerAttributeOperation("hostname", new ModelNode("new_hostname"));
        writeSyslogHandlerAttributeOperation("syslog-format", new ModelNode("RFC3164"));
    }

    private void writeSyslogHandlerAttributeOperation(String name, ModelNode value) throws IOException {
        ModelNode op = Util.getWriteAttributeOperation(syslogHandlerAddress, name, value);
        ModelNode result = managementClient.getControllerClient().execute(op);
        Assert.assertEquals(result.get("failure-description").asString(), SUCCESS, result.get(OUTCOME).asString());
    }

    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        return war;
    }

    static class SyslogHandlerTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode op;
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("syslog-handler", SYSLOG_HANDLER_NAME);
            op.get(LEVEL).set("TRACE");
            op.get(PORT).set(9876);
            op.get(APP_NAME).set("old_name");
            op.get(ENABLED).set("false");
            op.get(FACILITY).set("kernel");
            op.get(SERVER_ADDRESS).set("127.0.0.1");
            op.get(HOSTNAME).set("old_hostname");
            op.get(SYSLOG_FORMAT).set("RFC5424");
            managementClient.getControllerClient().execute(op);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode op;
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("syslog-handler", SYSLOG_HANDLER_NAME);
            managementClient.getControllerClient().execute(op);
        }

    }

}
