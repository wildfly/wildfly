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


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.ejb3.subsystem.EJB3Extension;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemModel;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.remoting.RemotingExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_IDENTITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_REF;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 * @author Jaikiran Pai
 */
public class EJBManagementUtil {

    private static final Logger logger = Logger.getLogger(EJBManagementUtil.class);

    /**
     * Returns the EJB remoting connector port that can be used for EJB remote invocations
     *
     * @param managementServerHostName The hostname of the server
     * @param managementPort           The management port
     * @return
     */
    public static int getEJBRemoteConnectorPort(final String managementServerHostName, final int managementPort, final CallbackHandler handler) {
        final ModelControllerClient modelControllerClient = getModelControllerClient(managementServerHostName, managementPort, handler);
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

    public static void createRemoteOutboundSocket(final String managementServerHostName, final int managementPort,
                                                  final String socketGroupName, final String outboundSocketName, final String destinationHost, final int destinationPort,
                                                  final CallbackHandler callbackHandler) {
        final ModelControllerClient modelControllerClient = getModelControllerClient(managementServerHostName, managementPort, callbackHandler);
        try {
            // /socket-binding-group=<group-name>/remote-destination-outbound-socket-binding=<name>:add(host=<host>, port=<port>)
            final ModelNode outboundSocketAddOperation = new ModelNode();
            outboundSocketAddOperation.get(OP).set(ADD);

            final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, socketGroupName),
                    PathElement.pathElement(ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING, outboundSocketName));
            outboundSocketAddOperation.get(OP_ADDR).set(address.toModelNode());
            // setup the other parameters for the add operation
            outboundSocketAddOperation.get(HOST).set(destinationHost);
            outboundSocketAddOperation.get(PORT).set(destinationPort);
            // execute the add operation
            execute(modelControllerClient, outboundSocketAddOperation);

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

    public static final void createSocketBinding(final ModelControllerClient modelControllerClient, final String name, int port) {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=" + name, ADD);
        op.get(NAME).set("port");
        op.get(VALUE).set(port);
        try {
            execute(modelControllerClient, op);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final void removeSocketBinding(final ModelControllerClient modelControllerClient, final String name) {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=" + name, REMOVE);
        try {
            execute(modelControllerClient, op);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createLocalOutboundSocket(final ModelControllerClient modelControllerClient,
                                                 final String socketGroupName, final String outboundSocketName,
                                                 final String socketBindingRef,
                                                 final CallbackHandler callbackHandler) {
        try {
            // /socket-binding-group=<group-name>/local-destination-outbound-socket-binding=<name>:add(socket-binding-ref=<ref>)
            final ModelNode outboundSocketAddOperation = new ModelNode();
            outboundSocketAddOperation.get(OP).set(ADD);

            final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, socketGroupName),
                    PathElement.pathElement(ModelDescriptionConstants.LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING, outboundSocketName));
            outboundSocketAddOperation.get(OP_ADDR).set(address.toModelNode());
            // setup the other parameters for the add operation
            outboundSocketAddOperation.get(SOCKET_BINDING_REF).set(socketBindingRef);
            // execute the add operation
            execute(modelControllerClient, outboundSocketAddOperation);

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void removeLocalOutboundSocket(final ModelControllerClient modelControllerClient,
                                                 final String socketGroupName, final String outboundSocketName,
                                                 final CallbackHandler callbackHandler) {
        try {
            // /socket-binding-group=<group-name>/local-destination-outbound-socket-binding=<name>:remove()
            final ModelNode outboundSocketRemoveOperation = new ModelNode();
            outboundSocketRemoveOperation.get(OP).set(REMOVE);

            final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SOCKET_BINDING_GROUP, socketGroupName),
                    PathElement.pathElement(ModelDescriptionConstants.LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING, outboundSocketName));
            outboundSocketRemoveOperation.get(OP_ADDR).set(address.toModelNode());
            outboundSocketRemoveOperation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);

            // execute the remove operation
            execute(modelControllerClient, outboundSocketRemoveOperation);

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void createRemoteOutboundConnection(final ModelControllerClient modelControllerClient,
                                                      final String connectionName, final String outboundSocketRef,
                                                      final Map<String, String> connectionCreationOptions, final CallbackHandler callbackHandler) {
        try {
            // /subsystem=remoting/remote-outbound-connection=<name>:add(outbound-socket-ref=<ref>)
            final ModelNode addRemoteOutboundConnection = new ModelNode();
            addRemoteOutboundConnection.get(OP).set(ADD);
            final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME),
                    PathElement.pathElement("remote-outbound-connection", connectionName));
            addRemoteOutboundConnection.get(OP_ADDR).set(address.toModelNode());


            final ModelNode addPasswordRealm = new ModelNode();
            addPasswordRealm.get(OP).set(ADD);
            ModelNode realmAddress = new ModelNode();
            realmAddress.add(CORE_SERVICE, MANAGEMENT);
            realmAddress.add(SECURITY_REALM, "PasswordRealm");
            addPasswordRealm.get(OP_ADDR).set(realmAddress);

            final ModelNode addServerIdentity = new ModelNode();
            addServerIdentity.get(OP).set(ADD);
            ModelNode secretAddress = realmAddress.clone().add(SERVER_IDENTITY, SECRET);
            addServerIdentity.get(OP_ADDR).set(secretAddress);
            addServerIdentity.get(VALUE).set("cGFzc3dvcmQx");

            // set the other properties
            addRemoteOutboundConnection.get("outbound-socket-binding-ref").set(outboundSocketRef);
            addRemoteOutboundConnection.get(SECURITY_REALM).set("PasswordRealm");
            addRemoteOutboundConnection.get("username").set("user1");
            addRemoteOutboundConnection.get("protocol").set("remote+http");

            final ModelNode op = Util.getEmptyOperation(COMPOSITE, new ModelNode());
            final ModelNode steps = op.get(STEPS);
            steps.add(addPasswordRealm);
            steps.add(addServerIdentity);
            steps.add(addRemoteOutboundConnection);


            // execute the add operation
            if (!connectionCreationOptions.isEmpty()) {
                for (Map.Entry<String, String> property : connectionCreationOptions.entrySet()) {
                    ModelNode propertyOp = new ModelNode();
                    propertyOp.get(OP).set(ADD);
                    propertyOp.get(OP_ADDR).set(address.toModelNode()).add("property", property.getKey());
                    propertyOp.get("value").set(property.getValue());
                    steps.add(propertyOp);
                }
            }
            execute(modelControllerClient, op);

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void removeRemoteOutboundConnection(final ModelControllerClient modelControllerClient,
                                                      final String connectionName, final CallbackHandler callbackHandler) {
        try {
            // /subsystem=remoting/remote-outbound-connection=<name>:remove()
            final ModelNode removeRemoteOutboundConnection = new ModelNode();
            removeRemoteOutboundConnection.get(OP).set(REMOVE);
            final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME),
                    PathElement.pathElement("remote-outbound-connection", connectionName));
            removeRemoteOutboundConnection.get(OP_ADDR).set(address.toModelNode());

            final ModelNode removeRealm = new ModelNode();
            removeRealm.get(OP).set(REMOVE);
            ModelNode realmAddress = new ModelNode();
            realmAddress.add(CORE_SERVICE, MANAGEMENT);
            realmAddress.add(SECURITY_REALM, "PasswordRealm");
            removeRealm.get(OP_ADDR).set(realmAddress);

            final ModelNode op = Util.getEmptyOperation(COMPOSITE, new ModelNode());
            final ModelNode steps = op.get(STEPS);
            steps.add(removeRemoteOutboundConnection);
            steps.add(removeRealm);

            // execute the remove operation
            execute(modelControllerClient, op);

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Creates a strict max pool in the EJB3 subsystem, with the passed <code>poolName</code> and pool attributes
     *
     * @param poolName    Pool name
     * @param maxPoolSize Max pool size
     * @param timeout     Instance acquisition timeout for the pool
     * @param unit        Instance acquisition timeout unit for the pool
     */
    public static void createStrictMaxPool(final ModelControllerClient modelControllerClient,
                                           final String poolName, final int maxPoolSize,
                                           final long timeout, final TimeUnit unit) {

        try {
            // first get the remote-connector from the EJB3 subsystem to find the remote connector ref
            // /subsystem=ejb3/strict-max-bean-instance-pool=<name>:add(....)
            final ModelNode addStrictMaxPool = new ModelNode();
            addStrictMaxPool.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
            final PathAddress strictMaxPoolAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME),
                    PathElement.pathElement(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL, poolName));
            addStrictMaxPool.get(OP_ADDR).set(strictMaxPoolAddress.toModelNode());

            // set the params
            addStrictMaxPool.get(EJB3SubsystemModel.MAX_POOL_SIZE).set(maxPoolSize);
            addStrictMaxPool.get(EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT).set(timeout);
            addStrictMaxPool.get(EJB3SubsystemModel.INSTANCE_ACQUISITION_TIMEOUT_UNIT).set(unit.name());

            // execute the add operation
            execute(modelControllerClient, addStrictMaxPool);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Removes an already created strict max pool from the EJB3 subsystem
     *
     * @param poolName The name of the pool to be removed
     */
    public static void removeStrictMaxPool(final ModelControllerClient controllerClient, final String poolName) {
        try {
            // /subsystem=ejb3/strict-max-bean-instance-pool=<name>:remove()
            final ModelNode removeStrictMaxPool = new ModelNode();
            removeStrictMaxPool.get(OP).set(REMOVE);
            final PathAddress strictMaxPoolAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME),
                    PathElement.pathElement(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL, poolName));
            removeStrictMaxPool.get(OP_ADDR).set(strictMaxPoolAddress.toModelNode());
            removeStrictMaxPool.get(ModelDescriptionConstants.OPERATION_HEADERS, ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART).set(true);

            // execute the remove operation
            execute(controllerClient, removeStrictMaxPool);

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void editEntityBeanInstancePool(final ModelControllerClient controllerClient, final String poolName, final boolean optimisticLocking) {
        try {
            // /subsystem=ejb3
            final PathAddress ejb3SubsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME));

            // /subsystem=ejb3:write-attribute(name="default-entity-bean-instance-pool", value=<poolName>)
            final ModelNode defaultEntityBeanInstancePool = new ModelNode();
            // set the operation
            defaultEntityBeanInstancePool.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            // set the address
            defaultEntityBeanInstancePool.get(OP_ADDR).set(ejb3SubsystemAddress.toModelNode());
            // setup the parameters for the write attribute operation
            defaultEntityBeanInstancePool.get(NAME).set(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_INSTANCE_POOL);
            defaultEntityBeanInstancePool.get(VALUE).set(poolName);
            // execute the operation
            execute(controllerClient, defaultEntityBeanInstancePool);

            // /subsystem=ejb3:write-attribute(name="default-entity-bean-optimistic-locking", value=<optimisticLocking>)
            final ModelNode defaultEntityBeanOptimisticLocking = new ModelNode();
            // set the operation
            defaultEntityBeanOptimisticLocking.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            // set the address
            defaultEntityBeanOptimisticLocking.get(OP_ADDR).set(ejb3SubsystemAddress.toModelNode());
            // setup the parameters for the write attribute operation
            defaultEntityBeanOptimisticLocking.get(NAME).set(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING);
            defaultEntityBeanOptimisticLocking.get(VALUE).set(optimisticLocking);
            // execute the operation
            execute(controllerClient, defaultEntityBeanOptimisticLocking);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void undefineEntityBeanInstancePool(final ModelControllerClient controllerClient) {
        try {
            // /subsystem=ejb3
            final PathAddress ejb3SubsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME));

            // /subsystem=ejb3:undefine-attribute(name="default-entity-bean-instance-pool")
            final ModelNode defaultEntityBeanInstancePool = new ModelNode();
            // set the operation
            defaultEntityBeanInstancePool.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            // set the address
            defaultEntityBeanInstancePool.get(OP_ADDR).set(ejb3SubsystemAddress.toModelNode());
            // setup the parameters for the undefine attribute operation
            defaultEntityBeanInstancePool.get(NAME).set(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_INSTANCE_POOL);
            // execute the operation
            execute(controllerClient, defaultEntityBeanInstancePool);

            // /subsystem=ejb3:undefine-attribute(name="default-entity-bean-optimistic-locking")
            final ModelNode defaultEntityBeanOptimisticLocking = new ModelNode();
            // set the operation
            defaultEntityBeanOptimisticLocking.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            // set the address
            defaultEntityBeanOptimisticLocking.get(OP_ADDR).set(ejb3SubsystemAddress.toModelNode());
            // setup the parameters for the undefine attribute operation
            defaultEntityBeanOptimisticLocking.get(NAME).set(EJB3SubsystemModel.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING);
            // execute the operation
            execute(controllerClient, defaultEntityBeanOptimisticLocking);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void enablePassByValueForRemoteInterfaceInvocations(ManagementClient managementClient) {
        editPassByValueForRemoteInterfaceInvocations(managementClient, true);
    }

    public static void disablePassByValueForRemoteInterfaceInvocations(ManagementClient managementClient) {
        editPassByValueForRemoteInterfaceInvocations(managementClient, false);
    }

    private static void editPassByValueForRemoteInterfaceInvocations(ManagementClient managementClient, final boolean passByValue) {
        final ModelControllerClient modelControllerClient = managementClient.getControllerClient();
        try {
            // /subsystem=ejb3:write-attribute(name="in-vm-remote-interface-invocation-pass-by-value", value=<passByValue>)
            final ModelNode passByValueWriteAttributeOperation = new ModelNode();
            // set the operation
            passByValueWriteAttributeOperation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            // set the address
            final PathAddress ejb3SubsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME));
            passByValueWriteAttributeOperation.get(OP_ADDR).set(ejb3SubsystemAddress.toModelNode());

            // setup the parameters for the write attribute operation
            passByValueWriteAttributeOperation.get(NAME).set(EJB3SubsystemModel.IN_VM_REMOTE_INTERFACE_INVOCATION_PASS_BY_VALUE);
            passByValueWriteAttributeOperation.get(VALUE).set(passByValue);

            // execute the operations
            execute(modelControllerClient, passByValueWriteAttributeOperation);

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void setDefaultDistinctName(ManagementClient managementClient, final String distinctName) {
        final ModelControllerClient modelControllerClient = managementClient.getControllerClient();
        try {
            final ModelNode op = new ModelNode();
            if (distinctName != null) {
                op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
                op.get(VALUE).set(distinctName);
            } else {
                op.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            }
            // set the address
            final PathAddress ejb3SubsystemAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME));
            op.get(OP_ADDR).set(ejb3SubsystemAddress.toModelNode());

            // setup the parameters for the write attribute operation
            op.get(NAME).set(EJB3SubsystemModel.DEFAULT_DISTINCT_NAME);

            // execute the operations
            execute(modelControllerClient, op);

        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static ModelControllerClient getModelControllerClient(final String managementServerHostName, final int managementPort, final CallbackHandler handler) {
        try {
            return ModelControllerClient.Factory.create(InetAddress.getByName(managementServerHostName), managementPort, handler);
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
            //logger.trace("Operation " + operation.toString() + " successful");
            return result;
        } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
            throw new RuntimeException(failureDesc);
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get(ClientConstants.OUTCOME));
        }

    }

    // TODO: This method is temporary hack till we figure out the management operation to get the
    // jboss.node.name system property from the server http://lists.jboss.org/pipermail/jboss-as7-dev/2011-November/004434.html
    public static String getNodeName() {
        // Logic copied from org.jboss.as.server.ServerEnvironment constructor
        final Properties props = System.getProperties();
        final Map<String, String> env = System.getenv();
        // Calculate host and default server name
        String hostName = props.getProperty("jboss.host.name");
        String qualifiedHostName = props.getProperty("jboss.qualified.host.name");
        if (qualifiedHostName == null) {
            // if host name is specified, don't pick a qualified host name that isn't related to it
            qualifiedHostName = hostName;
            if (qualifiedHostName == null) {
                // POSIX-like OSes including Mac should have this set
                qualifiedHostName = env.get("HOSTNAME");
            }
            if (qualifiedHostName == null) {
                // Certain versions of Windows
                qualifiedHostName = env.get("COMPUTERNAME");
            }
            if (qualifiedHostName == null) {
                try {
                    qualifiedHostName = NetworkUtils.canonize(InetAddress.getLocalHost().getHostName());
                } catch (UnknownHostException e) {
                    qualifiedHostName = null;
                }
            }
            if (qualifiedHostName != null && qualifiedHostName.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$|:")) {
                // IP address is not acceptable
                qualifiedHostName = null;
            }
            if (qualifiedHostName == null) {
                // Give up
                qualifiedHostName = "unknown-host.unknown-domain";
            }
            qualifiedHostName = qualifiedHostName.trim().toLowerCase(Locale.ENGLISH);
        }
        if (hostName == null) {
            // Use the host part of the qualified host name
            final int idx = qualifiedHostName.indexOf('.');
            hostName = idx == -1 ? qualifiedHostName : qualifiedHostName.substring(0, idx);
        }

        // Set up the server name for management purposes
        String serverName = props.getProperty("jboss.server.name");
        if (serverName == null) {
            serverName = hostName;
        }
        // Set up the clustering node name
        String nodeName = props.getProperty("jboss.node.name");
        if (nodeName == null) {
            nodeName = serverName;
        }
        return nodeName;
    }
}
