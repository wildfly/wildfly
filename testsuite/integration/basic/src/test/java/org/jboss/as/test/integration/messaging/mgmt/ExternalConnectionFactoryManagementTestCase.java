/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.ServerReload;
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
            attributes.get("connectors").add(CONNECTOR_NAME);
            jmsOperations.addJmsExternalConnectionFactory(CF_NAME, "java:/jms/" + CF_NAME, attributes);

            final ModelNode writeAttribute = new ModelNode();
            writeAttribute.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            writeAttribute.get(OP_ADDR).set(jmsOperations.getSubsystemAddress().add("connection-factory", CF_NAME));
            writeAttribute.get(NAME).set("discovery-group");
            writeAttribute.get(VALUE).set(randomUUID().toString());

            try {
                executeOperation(writeAttribute);
                fail("it is not possible to define a discovery group when the connector attribute is already defined");
            } catch (MgmtOperationException e) {
                assertEquals(FAILED, e.getResult().get(OUTCOME).asString());
                assertEquals(true, e.getResult().get(ROLLED_BACK).asBoolean());
                assertTrue(e.getResult().get(FAILURE_DESCRIPTION).asString().contains("WFLYCTL0105"));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                jmsOperations.removeJmsExternalConnectionFactory(CF_NAME);
            } catch (RuntimeException ex) {
                //ignore
            }
            try {
                jmsOperations.removeExternalHttpConnector(CONNECTOR_NAME);
            } catch (RuntimeException ex) {
                //ignore
            }
        }
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Test
    public void testRemovePooledConnectionFactory() throws Exception {
        //      /subsystem=messaging-activemq/http-connector=http-connector-remove-test:add(socket-binding=http, endpoint=http-acceptor)
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
//        jmsOperations.addExternalHttpConnector("http-connector-remove-test", "http", "http-acceptor");
        ModelNode connectorAddress = new ModelNode();
        connectorAddress.add("subsystem", "messaging-activemq");
        connectorAddress.add("http-connector", "http-connector-remove-test");
        ModelNode op = Operations.createAddOperation(connectorAddress);
        op.get("socket-binding").set("http");
        op.get("endpoint").set("http-acceptor");
        execute(managementClient.getControllerClient(), op);

//      /subsystem=messaging-activemq/pooled-connection-factory=pool3:add(connectors=[http-connector-test],entries=[foo])
        ModelNode pcfAddress = new ModelNode();
        pcfAddress.add("subsystem", "messaging-activemq");
        pcfAddress.add("pooled-connection-factory", "pool3");
        op = Operations.createAddOperation(pcfAddress);
        op.get("connectors").setEmptyList().add("http-connector-remove-test");
        op.get("entries").setEmptyList().add("foo");
        execute(managementClient.getControllerClient(), op);

//      /subsystem=messaging-activemq/pooled-connection-factory=pool3:remove()
        op = Operations.createRemoveOperation(pcfAddress);
        execute(managementClient.getControllerClient(), op);

        op = Operations.createRemoveOperation(connectorAddress);
        execute(managementClient.getControllerClient(), op);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    private static ModelNode execute(final ModelControllerClient client, final ModelNode op) throws IOException {
        final ModelNode result = client.execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException(Operations.getFailureDescription(result).asString());
        }
        return result;
    }
}
