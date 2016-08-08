/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.web.security.ssl;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Check some possible settings of https connector
 * 
 * Connected JIRAs: JBPAPP6-923, JBPAPP6-1456
 * 
 * @author olukas,
 */
@RunWith(Arquillian.class)
@ServerSetup(HttpsConnectorSettingsTestCase.HttpsConnectorSettingsTestCaseSetup.class)
@RunAsClient
public class HttpsConnectorSettingsTestCase {

    static class HttpsConnectorSettingsTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            managementClientForTest = managementClient;

            ModelNode op;

            // create new https connector
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "web");
            op.get(OP_ADDR).add("connector", "https");
            op.get("protocol").set("HTTP/1.1");
            op.get("scheme").set("https");
            op.get("socket-binding").set("https");
            op.get("secure").set("true");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());

            // set up https connector
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "web");
            op.get(OP_ADDR).add("connector", "https");
            op.get(OP_ADDR).add("ssl", "configuration");
            op.get("name").set("https");
            op.get("password").set("pass");
            op.get("keystore-type").set("PKCS11");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode op;

            // remove created https connector
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "web");
            op.get(OP_ADDR).add("connector", "https");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            managementClient.getControllerClient().execute(new OperationBuilder(op).build());
        }

    }

    static ManagementClient managementClientForTest;

    /*
     * JBPAPP6-923 Check that key-alias is undefined
     */
    @Test
    public void testAbleToNotSetKeyAliasForSSL() throws Exception {
        ModelNode op;
        op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get(OP_ADDR).add("connector", "https");
        op.get(OP_ADDR).add("ssl", "configuration");
        op.get("name").set("key-alias");
        ModelNode result = (managementClientForTest.getControllerClient().execute(new OperationBuilder(op).build()))
                .get(RESULT);
        assertEquals(result.toString(), "undefined");
    }

    /*
     * JBPAPP6-1456 Check that keystore-type is set to PKCS11
     */
    @Test
    public void testAbleToSetPKCS11() throws Exception {
        ModelNode op;
        op = new ModelNode();
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get(OP_ADDR).add("connector", "https");
        op.get(OP_ADDR).add("ssl", "configuration");
        op.get("name").set("keystore-type");
        ModelNode result = (managementClientForTest.getControllerClient().execute(new OperationBuilder(op).build()))
                .get(RESULT);
        assertEquals(result.asString(), "PKCS11");
    }

    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        return war;
    }
}
