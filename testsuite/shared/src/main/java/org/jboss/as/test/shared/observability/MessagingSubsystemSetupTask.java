/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability;

import java.util.List;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

public class MessagingSubsystemSetupTask implements ServerSetupTask {
    private static final String MESSAGING_EXTENSION = "org.wildfly.extension.messaging-activemq";
    private static final String MESSAGING_SUBSYSTEM = "messaging-activemq";
    private static final ModelNode EXTENSION_ADDRESS = Operations.createAddress("extension", MESSAGING_EXTENSION);
    private static final ModelNode SUBSYSTEM_ADDRESS = Operations.createAddress("subsystem", MESSAGING_SUBSYSTEM);
    private static final ModelNode SERVER_ADDRESS = Operations.createAddress(
            "subsystem", MESSAGING_SUBSYSTEM, "server", "default");
    private static final ModelNode CONNECTION_FACTORY_ADDRESS = Operations.createAddress(
            "subsystem", MESSAGING_SUBSYSTEM, "server", "default", "pooled-connection-factory", "activemq-ra");
    private static final ModelNode IN_VM_CONNECTOR_ADDRESS = Operations.createAddress(
            "subsystem", MESSAGING_SUBSYSTEM, "server", "default", "in-vm-connector", "in-vm");
    private static final ModelNode IN_VM_ACCEPTOR_ADDRESS = Operations.createAddress(
            "subsystem", MESSAGING_SUBSYSTEM, "server", "default", "in-vm-acceptor", "in-vm");

    private boolean extensionAdded;
    private boolean subsystemAdded;
    private boolean serverAdded;
    private boolean connectionFactoryAdded;
    private boolean inVmConnectorAdded;
    private boolean inVmAcceptorAdded;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        if (!containsChild(managementClient, "extension", MESSAGING_EXTENSION)) {
            executeCli(managementClient, "/extension=%s:add", MESSAGING_EXTENSION);
            extensionAdded = true;
        }
        if (!resourceExists(managementClient, SUBSYSTEM_ADDRESS)) {
            executeCli(managementClient, "/subsystem=%s:add", MESSAGING_SUBSYSTEM);
            subsystemAdded = true;
        }
        if (extensionAdded || subsystemAdded) {
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
        if (!resourceExists(managementClient, SERVER_ADDRESS)) {
            executeCli(managementClient,
                    "/subsystem=%s/server=default:add(elytron-domain=ApplicationDomain,statistics-enabled=true)",
                    MESSAGING_SUBSYSTEM);
            serverAdded = true;
        }
        if (!resourceExists(managementClient, IN_VM_CONNECTOR_ADDRESS)) {
            executeCli(managementClient,
                    "/subsystem=%s/server=default/in-vm-connector=in-vm:add(server-id=0)", MESSAGING_SUBSYSTEM);
            inVmConnectorAdded = true;
        }
        if (!resourceExists(managementClient, IN_VM_ACCEPTOR_ADDRESS)) {
            executeCli(managementClient,
                    "/subsystem=%s/server=default/in-vm-acceptor=in-vm:add(server-id=0)", MESSAGING_SUBSYSTEM);
            inVmAcceptorAdded = true;
        }
        if (!resourceExists(managementClient, CONNECTION_FACTORY_ADDRESS)) {
            executeCli(managementClient,
                    "/subsystem=%s/server=default/pooled-connection-factory=activemq-ra:add(" +
                            "entries=[java:/JmsXA,java:jboss/DefaultJMSConnectionFactory]," +
                            "connectors=[in-vm],transaction=xa)", MESSAGING_SUBSYSTEM);
            connectionFactoryAdded = true;
        }
        if (serverAdded || inVmConnectorAdded || inVmAcceptorAdded || connectionFactoryAdded) {
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (connectionFactoryAdded && resourceExists(managementClient, CONNECTION_FACTORY_ADDRESS)) {
            executeCli(managementClient, "/subsystem=%s/server=default/pooled-connection-factory=activemq-ra:remove", MESSAGING_SUBSYSTEM);
        }
        if (inVmAcceptorAdded && resourceExists(managementClient, IN_VM_ACCEPTOR_ADDRESS)) {
            executeCli(managementClient, "/subsystem=%s/server=default/in-vm-acceptor=in-vm:remove", MESSAGING_SUBSYSTEM);
        }
        if (inVmConnectorAdded && resourceExists(managementClient, IN_VM_CONNECTOR_ADDRESS)) {
            executeCli(managementClient, "/subsystem=%s/server=default/in-vm-connector=in-vm:remove", MESSAGING_SUBSYSTEM);
        }
        if (serverAdded && resourceExists(managementClient, SERVER_ADDRESS)) {
            executeCli(managementClient, "/subsystem=%s/server=default:remove", MESSAGING_SUBSYSTEM);
        }
        if (subsystemAdded && resourceExists(managementClient, SUBSYSTEM_ADDRESS)) {
            executeCli(managementClient, "/subsystem=%s:remove", MESSAGING_SUBSYSTEM);
        }
        if (extensionAdded && resourceExists(managementClient, EXTENSION_ADDRESS)) {
            executeCli(managementClient, "/extension=%s:remove", MESSAGING_EXTENSION);
        }
        if (connectionFactoryAdded || inVmAcceptorAdded || inVmConnectorAdded || serverAdded
                || subsystemAdded || extensionAdded) {
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }
    }

    private static boolean resourceExists(ManagementClient managementClient, ModelNode address) throws Exception {
        return Operations.isSuccessfulOutcome(managementClient.getControllerClient()
                .execute(Operations.createReadResourceOperation(address)));
    }

    private static boolean containsChild(ManagementClient managementClient, String childType, String childName)
            throws Exception {
        ModelNode operation = new ModelNode();
        operation.get("operation").set("read-children-names");
        operation.get("child-type").set(childType);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new IllegalStateException(result.toString());
        }
        List<ModelNode> names = result.get("result").asList();
        return names.stream().anyMatch(name -> childName.equals(name.asString()));
    }

    private static void executeCli(ManagementClient managementClient, String command, Object... arguments)
            throws Exception {
        ModelNode operation = CLITestUtil.getCommandContext().buildRequest(String.format(command, arguments));
        ModelNode result = managementClient.getControllerClient().execute(operation);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new IllegalStateException(Operations.getFailureDescription(result).asString());
        }
    }
}
