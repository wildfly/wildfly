/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.common;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.subsystem.EJB3Extension;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemModel;
import org.jboss.as.remoting.RemotingExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * @author Jaikiran Pai
 */
public class EJBRemoteManagementUtil {

    private static final Logger logger = Logger.getLogger(EJBRemoteManagementUtil.class);

    /**
     * Returns the EJB remoting connector port that can be used for EJB remote invocations
     * 
     * @param managementServerHostName The hostname of the server
     * @param managementPort The management port
     * @return
     */
    public static int getEJBRemoteConnectorPort(final String managementServerHostName, final int managementPort) {
        final ModelControllerClient modelControllerClient = getModelControllerClient(managementServerHostName, managementPort);
        try {
            // first get the remote-connector from the EJB3 subsystem to find the remote connector ref
            // /subsystem=ejb3/service=remote:read-attribute(name=connector-ref)
            final ModelNode readConnectorRefAttribute = new ModelNode();
            readConnectorRefAttribute.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
            readConnectorRefAttribute.get(NAME).set(EJB3SubsystemModel.CONNECTOR_REF);

            final PathAddress ejbRemotingServiceAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME),
                    EJB3SubsystemModel.REMOTE_SERVICE_PATH);
            readConnectorRefAttribute.get(OP_ADDR).set(ejbRemotingServiceAddress.toModelNode());

            // execute the read-attribute
            final ModelNode connectorRefResult = execute(modelControllerClient, readConnectorRefAttribute);
            final String connectorRef = connectorRefResult.get(RESULT).asString();

            // now get the socket-binding ref for this connector ref, from the remoting subsystem
            // /subsystem=remoting/connector=<connector-ref>:read-attribute(name=socket-binding)
            final ModelNode readSocketBindingRefAttribute = new ModelNode();
            readSocketBindingRefAttribute.get(OP).set(READ_ATTRIBUTE_OPERATION);
            readSocketBindingRefAttribute.get(NAME).set(SOCKET_BINDING);

            final PathAddress remotingSubsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME),
                    PathElement.pathElement("connector", connectorRef));
            readSocketBindingRefAttribute.get(OP_ADDR).set(remotingSubsystemAddress.toModelNode());

            // execute the read-attribute
            final ModelNode socketBindingRefResult = execute(modelControllerClient, readSocketBindingRefAttribute);
            final String socketBindingRef = socketBindingRefResult.get(RESULT).asString();

            // now get the port value of that socket binding ref
            // /socket-binding-group=standard-sockets/socket-binding=<socket-binding-ref>:read-attribute(name=port)
            final ModelNode readPortAttribute = new ModelNode();
            readPortAttribute.get(OP).set(READ_ATTRIBUTE_OPERATION);
            readPortAttribute.get(NAME).set(PORT);
            // TODO: "standard-sockets" group is hardcoded for now
            final PathAddress socketBindingAddress = PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, "standard-sockets"),
                    PathElement.pathElement(SOCKET_BINDING, socketBindingRef));
            readPortAttribute.get(OP_ADDR).set(socketBindingAddress.toModelNode());

            // execute the read-attribute
            final ModelNode portResult = execute(modelControllerClient, readPortAttribute);
            return portResult.get(RESULT).asInt();

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            // close the controller client connection
            try {
                modelControllerClient.close();
            } catch (IOException e) {
                logger.warn("Error closing model controller client", e);
            }
        }
    }

    private static ModelControllerClient getModelControllerClient(final String managementServerHostName, final int managementPort) {
        try {
            return ModelControllerClient.Factory.create(InetAddress.getByName(managementServerHostName), managementPort);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Cannot create model controller client for host: " + managementServerHostName + " and port " + managementPort, e);
        }
    }

    /**
     * Executes the operation and returns the result if successful. Else throws an exception
     * 
     * @param modelControllerClient
     * @param operation
     * @return
     * @throws IOException
     */
    private static ModelNode execute(final ModelControllerClient modelControllerClient, final ModelNode operation) throws IOException {
        final ModelNode result = modelControllerClient.execute(operation);
        if (result.hasDefined(ClientConstants.OUTCOME) && ClientConstants.SUCCESS.equals(result.get(ClientConstants.OUTCOME).asString())) {
            logger.info("Operation " + operation.toString() + " successful");
            return result;
        } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
            throw new RuntimeException(failureDesc);
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get(ClientConstants.OUTCOME));
        }

    }
}
