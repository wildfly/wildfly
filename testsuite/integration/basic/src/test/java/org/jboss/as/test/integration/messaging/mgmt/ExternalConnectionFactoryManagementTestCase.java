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

package org.jboss.as.test.integration.messaging.mgmt;

import static java.util.UUID.randomUUID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jgroups.util.StackType;
import org.jgroups.util.Util;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c)2012 Red Hat, inc
 *
 * https://issues.jboss.org/browse/AS7-5107
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ExternalConnectionFactoryManagementTestCase extends ContainerResourceMgmtTestBase {

    private static final String CF_NAME = randomUUID().toString();
    private static final String CONNECTOR_NAME = "client-http-connector";

    @ContainerResource
    private ManagementClient managementClient;

    @BeforeClass
    public static void beforeClass() {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Assume.assumeFalse("[WFCI-32] Disable on Windows+IPv6 until CI environment is fixed", Util.checkForWindows() && (Util.getIpStackType() == StackType.IPv6));
            return null;
        });
    }

    @Test
    public void testWriteDiscoveryGroupAttributeWhenConnectorIsAlreadyDefined() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        try {
            jmsOperations.addExternalHttpConnector(CONNECTOR_NAME, "http", "http-acceptor");

            ModelNode attributes = new ModelNode();
            attributes.get(CommonAttributes.CONNECTORS).add(CONNECTOR_NAME);
            jmsOperations.addJmsExternalConnectionFactory(CF_NAME, "java:/jms/" + CF_NAME, attributes);

            final ModelNode writeAttribute = new ModelNode();
            writeAttribute.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            writeAttribute.get(OP_ADDR).set(jmsOperations.getSubsystemAddress().add("connection-factory", CF_NAME));
            writeAttribute.get(NAME).set(CommonAttributes.DISCOVERY_GROUP);
            writeAttribute.get(VALUE).set(randomUUID().toString());

            try {
                executeOperation(writeAttribute);
                fail("it is not possible to define a discovery group when the connector attribute is already defined");
            } catch (MgmtOperationException e) {
                assertEquals(FAILED, e.getResult().get(OUTCOME).asString());
                assertEquals(true, e.getResult().get(ROLLED_BACK).asBoolean());
                assertTrue(e.getResult().get(FAILURE_DESCRIPTION).asString().contains("WFLYCTL0105"));
            }
        } finally {
            jmsOperations.removeJmsExternalConnectionFactory(CF_NAME);
            jmsOperations.removeExternalHttpConnector(CONNECTOR_NAME);
        }
        ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
    }

}
