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

package org.wildfly.extension.messaging.activemq;

import static org.apache.activemq.artemis.core.remoting.impl.invm.TransportConstants.SERVER_ID_PROP_NAME;
import static org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.ACTIVEMQ_SERVER_NAME;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.FACTORY_CLASS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.IN_VM_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.IN_VM_CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REMOTE_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REMOTE_CONNECTOR;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.ClientMapping;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.StartException;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Basic {@link TransportConfiguration} (Acceptor/Connector) related operations.
 *
 * Artemis changed the naming convention for naming its parameters and uses CamelCase names.
 * WildFly convention is to use hyphen-separated names. The mapping is done when creating Artemis connector/acceptor
 * configuration based on the WildFly management model.
 *
 * @author Emanuel Muckenhuber
 */
public class TransportConfigOperationHandlers {

    private static final Map<String, String> CONNECTORS_KEYS_MAP = new HashMap<>();
    private static final Map<String, String> ACCEPTOR_KEYS_MAP = new HashMap<>();

    private static final String BATCH_DELAY = "batch-delay";
    private static final String HTTP_UPGRADE_ENABLED = "http-upgrade-enabled";
    private static final String KEY_STORE_PASSWORD = "key-store-password";
    private static final String KEY_STORE_PATH = "key-store-path";
    private static final String KEY_STORE_PROVIDER = "key-store-provider";
    private static final String TCP_RECEIVE_BUFFER_SIZE = "tcp-receive-buffer-size";
    private static final String TCP_SEND_BUFFER_SIZE = "tcp-send-buffer-size";
    private static final String TRUST_STORE_PASSWORD = "trust-store-password";
    private static final String TRUST_STORE_PATH = "trust-store-path";
    private static final String TRUST_STORE_PROVIDER = "trust-store-provider";
    private static final String ENABLED_PROTOCOLS = "enabled-protocols";
    private static final String ENABLED_CIPHER_SUITES = "enabled-cipher-suites";
    private static final String HOST = "host";
    private static final String PORT = "port";
    public static final String SSL_ENABLED = "ssl-enabled";
    public static final String USE_NIO = "use-nio";
    public static final String TCP_NO_DELAY = "tcp-no-delay";

    /**
     * The name of the SocketBinding reference to use for HOST/PORT
     * configuration
     */
    private static final String SOCKET_REF = RemoteTransportDefinition.SOCKET_BINDING.getName();

    static {
        CONNECTORS_KEYS_MAP.put(InVMTransportDefinition.SERVER_ID.getName(),
                org.apache.activemq.artemis.core.remoting.impl.invm.TransportConstants.SERVER_ID_PROP_NAME);
        CONNECTORS_KEYS_MAP.put("buffer-pooling", org.apache.activemq.artemis.core.remoting.impl.invm.TransportConstants.BUFFER_POOLING);
        CONNECTORS_KEYS_MAP.put(SSL_ENABLED,
                TransportConstants.SSL_ENABLED_PROP_NAME);
        CONNECTORS_KEYS_MAP.put("http-enabled",
                TransportConstants.HTTP_ENABLED_PROP_NAME);
        CONNECTORS_KEYS_MAP.put("http-client-idle-time",
                TransportConstants.HTTP_CLIENT_IDLE_PROP_NAME);
        CONNECTORS_KEYS_MAP.put("http-client-idle-scan-period",
                TransportConstants.HTTP_CLIENT_IDLE_SCAN_PERIOD);
        CONNECTORS_KEYS_MAP.put("http-requires-session-id",
                TransportConstants.HTTP_REQUIRES_SESSION_ID);
        CONNECTORS_KEYS_MAP.put(HTTP_UPGRADE_ENABLED,
                TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME);
        CONNECTORS_KEYS_MAP.put("http-upgrade-endpoint",
                TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME);
        CONNECTORS_KEYS_MAP.put("use-servlet",
                TransportConstants.USE_SERVLET_PROP_NAME);
        CONNECTORS_KEYS_MAP.put("servlet-path",
                TransportConstants.SERVLET_PATH);
        CONNECTORS_KEYS_MAP.put(USE_NIO,
                TransportConstants.USE_NIO_PROP_NAME);
        CONNECTORS_KEYS_MAP.put("use-nio-global-worker-pool",
                TransportConstants.USE_NIO_GLOBAL_WORKER_POOL_PROP_NAME);
        CONNECTORS_KEYS_MAP.put(HOST,
                TransportConstants.HOST_PROP_NAME);
        CONNECTORS_KEYS_MAP.put(PORT,
                TransportConstants.PORT_PROP_NAME);
        CONNECTORS_KEYS_MAP.put("local-address",
                TransportConstants.LOCAL_ADDRESS_PROP_NAME);
        CONNECTORS_KEYS_MAP.put("local-port",
                TransportConstants.LOCAL_PORT_PROP_NAME);
        CONNECTORS_KEYS_MAP.put(KEY_STORE_PROVIDER,
                TransportConstants.KEYSTORE_PROVIDER_PROP_NAME);
        CONNECTORS_KEYS_MAP.put(KEY_STORE_PATH,
                TransportConstants.KEYSTORE_PATH_PROP_NAME);
        CONNECTORS_KEYS_MAP.put(KEY_STORE_PASSWORD,
                TransportConstants.KEYSTORE_PASSWORD_PROP_NAME);
        CONNECTORS_KEYS_MAP.put(TRUST_STORE_PROVIDER,
                TransportConstants.TRUSTSTORE_PROVIDER_PROP_NAME);
        CONNECTORS_KEYS_MAP.put(TRUST_STORE_PATH,
                TransportConstants.TRUSTSTORE_PATH_PROP_NAME);
        CONNECTORS_KEYS_MAP.put(TRUST_STORE_PASSWORD,
                TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME);
        CONNECTORS_KEYS_MAP.put(ENABLED_CIPHER_SUITES,
                TransportConstants.ENABLED_CIPHER_SUITES_PROP_NAME);
        CONNECTORS_KEYS_MAP.put(ENABLED_PROTOCOLS,
                TransportConstants.ENABLED_PROTOCOLS_PROP_NAME);
        CONNECTORS_KEYS_MAP.put(TCP_NO_DELAY,
                TransportConstants.TCP_NODELAY_PROPNAME);
        CONNECTORS_KEYS_MAP.put(TCP_SEND_BUFFER_SIZE,
                TransportConstants.TCP_SENDBUFFER_SIZE_PROPNAME);
        CONNECTORS_KEYS_MAP.put(TCP_RECEIVE_BUFFER_SIZE,
                TransportConstants.TCP_RECEIVEBUFFER_SIZE_PROPNAME);
        CONNECTORS_KEYS_MAP.put("nio-remoting-threads",
                TransportConstants.NIO_REMOTING_THREADS_PROPNAME);
        CONNECTORS_KEYS_MAP.put(BATCH_DELAY,
                TransportConstants.BATCH_DELAY);
        CONNECTORS_KEYS_MAP.put("connect-timeout-millis",
                TransportConstants.NETTY_CONNECT_TIMEOUT);

        ACCEPTOR_KEYS_MAP.put(InVMTransportDefinition.SERVER_ID.getName(),
                org.apache.activemq.artemis.core.remoting.impl.invm.TransportConstants.SERVER_ID_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put(BATCH_DELAY,
                TransportConstants.BATCH_DELAY);
        ACCEPTOR_KEYS_MAP.put("buffer-pooling", org.apache.activemq.artemis.core.remoting.impl.invm.TransportConstants.BUFFER_POOLING);
        ACCEPTOR_KEYS_MAP.put("cluster-connection",
                TransportConstants.CLUSTER_CONNECTION);
        ACCEPTOR_KEYS_MAP.put("connection-ttl",
                TransportConstants.CONNECTION_TTL);
        ACCEPTOR_KEYS_MAP.put("connections-allowed",
                TransportConstants.CONNECTIONS_ALLOWED);
        ACCEPTOR_KEYS_MAP.put("direct-deliver",
                TransportConstants.DIRECT_DELIVER);
        ACCEPTOR_KEYS_MAP.put(ENABLED_CIPHER_SUITES,
                TransportConstants.ENABLED_CIPHER_SUITES_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put(ENABLED_PROTOCOLS,
                TransportConstants.ENABLED_PROTOCOLS_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put(HOST,
                TransportConstants.HOST_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put("http-response-time",
                TransportConstants.HTTP_RESPONSE_TIME_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put("http-server-scan-period",
                TransportConstants.HTTP_SERVER_SCAN_PERIOD_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put(HTTP_UPGRADE_ENABLED,
                TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put(KEY_STORE_PASSWORD,
                TransportConstants.KEYSTORE_PASSWORD_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put(KEY_STORE_PATH,
                TransportConstants.KEYSTORE_PATH_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put(KEY_STORE_PROVIDER,
                TransportConstants.KEYSTORE_PROVIDER_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put("needs-client-auth",
                TransportConstants.NEED_CLIENT_AUTH_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put("nio-remoting-threads",
                TransportConstants.NIO_REMOTING_THREADS_PROPNAME);
        ACCEPTOR_KEYS_MAP.put(PORT,
                TransportConstants.PORT_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put("protocols",
                TransportConstants.PROTOCOLS_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put(SSL_ENABLED,
                TransportConstants.SSL_ENABLED_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put("stomp-enable-message-id",
                TransportConstants.STOMP_ENABLE_MESSAGE_ID);
        ACCEPTOR_KEYS_MAP.put("stomp-min-large-message-size",
                TransportConstants.STOMP_MIN_LARGE_MESSAGE_SIZE);
        ACCEPTOR_KEYS_MAP.put("stomp-consumer-credits",
                TransportConstants.STOMP_CONSUMERS_CREDIT);
        ACCEPTOR_KEYS_MAP.put(TCP_NO_DELAY,
                TransportConstants.TCP_NODELAY_PROPNAME);
        ACCEPTOR_KEYS_MAP.put(TCP_RECEIVE_BUFFER_SIZE,
                TransportConstants.TCP_RECEIVEBUFFER_SIZE_PROPNAME);
        ACCEPTOR_KEYS_MAP.put(TCP_SEND_BUFFER_SIZE,
                TransportConstants.TCP_SENDBUFFER_SIZE_PROPNAME);
        ACCEPTOR_KEYS_MAP.put(TRUST_STORE_PASSWORD,
                TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put(TRUST_STORE_PATH,
                TransportConstants.TRUSTSTORE_PATH_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put(TRUST_STORE_PROVIDER,
                TransportConstants.TRUSTSTORE_PROVIDER_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put("use-invm",
                TransportConstants.USE_INVM_PROP_NAME);
        ACCEPTOR_KEYS_MAP.put(USE_NIO,
                TransportConstants.USE_NIO_PROP_NAME);
    }

    /**
     * Process the acceptor information.
     *
     * @param context       the operation context
     * @param configuration the ActiveMQ configuration
     * @param params        the detyped operation parameters
     * @param bindings      the referenced socket bindings
     * @throws OperationFailedException
     */
    static void processAcceptors(final OperationContext context, final Configuration configuration, final ModelNode params, final Set<String> bindings) throws OperationFailedException {
        final Map<String, TransportConfiguration> acceptors = new HashMap<String, TransportConfiguration>();
        if (params.hasDefined(ACCEPTOR)) {
            for (final Property property : params.get(ACCEPTOR).asPropertyList()) {
                final String acceptorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config, ACCEPTOR_KEYS_MAP);
                final String clazz = config.get(FACTORY_CLASS.getName()).asString();
                ModelNode socketBinding = GenericTransportDefinition.SOCKET_BINDING.resolveModelAttribute(context, config);
                if (socketBinding.isDefined()) {
                    bindings.add(socketBinding.asString());
                    // uses the parameters to pass the socket binding name that will be read in ActiveMQServerService.start()
                    parameters.put(GenericTransportDefinition.SOCKET_BINDING.getName(), socketBinding.asString());
                }
                acceptors.put(acceptorName, new TransportConfiguration(clazz, parameters, acceptorName));
            }
        }
        if (params.hasDefined(REMOTE_ACCEPTOR)) {
            for (final Property property : params.get(REMOTE_ACCEPTOR).asPropertyList()) {
                final String acceptorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config, ACCEPTOR_KEYS_MAP);
                final String binding = config.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).asString();
                bindings.add(binding);
                // uses the parameters to pass the socket binding name that will be read in ActiveMQServerService.start()
                parameters.put(RemoteTransportDefinition.SOCKET_BINDING.getName(), binding);
                acceptors.put(acceptorName, new TransportConfiguration(NettyAcceptorFactory.class.getName(), parameters, acceptorName));
            }
        }
        if (params.hasDefined(IN_VM_ACCEPTOR)) {
            for (final Property property : params.get(IN_VM_ACCEPTOR).asPropertyList()) {
                final String acceptorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config, ACCEPTOR_KEYS_MAP);
                parameters.put(SERVER_ID_PROP_NAME, InVMTransportDefinition.SERVER_ID.resolveModelAttribute(context, config).asInt());
                acceptors.put(acceptorName, new TransportConfiguration(InVMAcceptorFactory.class.getName(), parameters, acceptorName));
            }
        }
        if (params.hasDefined(HTTP_ACCEPTOR)) {
            for (final Property property : params.get(HTTP_ACCEPTOR).asPropertyList()) {
                final String acceptorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config, ACCEPTOR_KEYS_MAP);
                parameters.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
                acceptors.put(acceptorName, new TransportConfiguration(NettyAcceptorFactory.class.getName(), parameters, acceptorName));
            }
        }
        configuration.setAcceptorConfigurations(new HashSet<TransportConfiguration>(acceptors.values()));
    }

    /**
     * Get the parameters.
     *
     * @param context the operation context
     * @param config the transport configuration
     * @param mapping Mapping betwen WildFly parameters (keys) and Artemis constants (values)
     * @return the extracted parameters
     * @throws OperationFailedException if an expression can not be resolved
     */
    public static Map<String, Object> getParameters(final OperationContext context, final ModelNode config, final Map<String, String> mapping) throws OperationFailedException {
        Map<String, String> fromModel = CommonAttributes.PARAMS.unwrap(context, config);
        Map<String, Object> parameters = new HashMap<>();
        for (Map.Entry<String, String> entry : fromModel.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            parameters.put(mapping.getOrDefault(key, key), value);
        }
        return parameters;
    }

    /**
     * Process the connector information.
     *
     * @param context       the operation context
     * @param configuration the ActiveMQ configuration
     * @param params        the detyped operation parameters
     * @param bindings      the referenced socket bindings
     * @throws OperationFailedException
     */
    static void processConnectors(final OperationContext context, final Configuration configuration, final ModelNode params, final Set<String> bindings) throws OperationFailedException {
        final Map<String, TransportConfiguration> connectors = new HashMap<String, TransportConfiguration>();
        if (params.hasDefined(CONNECTOR)) {
            for (final Property property : params.get(CONNECTOR).asPropertyList()) {
                final String connectorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config, CONNECTORS_KEYS_MAP);
                ModelNode socketBinding = GenericTransportDefinition.SOCKET_BINDING.resolveModelAttribute(context, config);
                if (socketBinding.isDefined()) {
                    bindings.add(socketBinding.asString());
                    // uses the parameters to pass the socket binding name that will be read in ActiveMQServerService.start()
                    parameters.put(GenericTransportDefinition.SOCKET_BINDING.getName(), socketBinding.asString());
                }
                final String clazz = FACTORY_CLASS.resolveModelAttribute(context, config).asString();
                connectors.put(connectorName, new TransportConfiguration(clazz, parameters, connectorName));
            }
        }
        if (params.hasDefined(REMOTE_CONNECTOR)) {
            for (final Property property : params.get(REMOTE_CONNECTOR).asPropertyList()) {
                final String connectorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config, CONNECTORS_KEYS_MAP);
                final String binding = config.get(RemoteTransportDefinition.SOCKET_BINDING.getName()).asString();
                bindings.add(binding);
                // uses the parameters to pass the socket binding name that will be read in ActiveMQServerService.start()
                parameters.put(RemoteTransportDefinition.SOCKET_BINDING.getName(), binding);
                connectors.put(connectorName, new TransportConfiguration(NettyConnectorFactory.class.getName(), parameters, connectorName));
            }
        }
        if (params.hasDefined(IN_VM_CONNECTOR)) {
            for (final Property property : params.get(IN_VM_CONNECTOR).asPropertyList()) {
                final String connectorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config, CONNECTORS_KEYS_MAP);
                parameters.put(CONNECTORS_KEYS_MAP.get(InVMTransportDefinition.SERVER_ID.getName()), InVMTransportDefinition.SERVER_ID.resolveModelAttribute(context, config).asInt());
                connectors.put(connectorName, new TransportConfiguration(InVMConnectorFactory.class.getName(), parameters, connectorName));
            }
        }
        if (params.hasDefined(HTTP_CONNECTOR)) {
            for (final Property property : params.get(HTTP_CONNECTOR).asPropertyList()) {
                final String connectorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = getParameters(context, config, CONNECTORS_KEYS_MAP);

                final String binding = HTTPConnectorDefinition.SOCKET_BINDING.resolveModelAttribute(context, config).asString();
                bindings.add(binding);
                // ARTEMIS-803 Artemis knows that is must not offset the HTTP port when it is used by colocated backups
                parameters.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
                parameters.put(TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME, HTTPConnectorDefinition.ENDPOINT.resolveModelAttribute(context, config).asString());
                // uses the parameters to pass the socket binding name that will be read in ActiveMQServerService.start()
                parameters.put(HTTPConnectorDefinition.SOCKET_BINDING.getName(), binding);
                ModelNode serverNameModelNode = HTTPConnectorDefinition.SERVER_NAME.resolveModelAttribute(context, config);
                // use the name of this server if the server-name attribute is undefined
                String serverName = serverNameModelNode.isDefined() ? serverNameModelNode.asString() : configuration.getName();
                parameters.put(ACTIVEMQ_SERVER_NAME, serverName);

                connectors.put(connectorName, new TransportConfiguration(NettyConnectorFactory.class.getName(), parameters, connectorName));
            }
        }
        configuration.setConnectorConfigurations(connectors);
    }

    public static TransportConfiguration[] processConnectors(final OperationContext context, final Collection<String> names, Set<String> bindings) throws OperationFailedException {
        final List<TransportConfiguration> connectors = new ArrayList<>();
        final PathAddress subsystemAddress = context.getCurrentAddress().getParent();
        Resource subsystemResource = context.readResourceFromRoot(subsystemAddress, false);
        for (String connectorName : names) {
            if (subsystemResource.hasChild(PathElement.pathElement(CommonAttributes.CONNECTOR, connectorName))) {
                final ModelNode config = context.readResourceFromRoot(subsystemAddress.append(PathElement.pathElement(CommonAttributes.CONNECTOR, connectorName)), true).getModel();
                final Map<String, Object> parameters = getParameters(context, config, CONNECTORS_KEYS_MAP);
                ModelNode socketBinding = GenericTransportDefinition.SOCKET_BINDING.resolveModelAttribute(context, config);
                if (socketBinding.isDefined()) {
                    // uses the parameters to pass the socket binding name that will be read in ActiveMQServerService.start()
                    parameters.put(GenericTransportDefinition.SOCKET_BINDING.getName(), socketBinding.asString());
                    bindings.add(socketBinding.asString());
                }
                final String clazz = FACTORY_CLASS.resolveModelAttribute(context, config).asString();
                connectors.add(new TransportConfiguration(clazz, parameters, connectorName));
            }
            if (subsystemResource.hasChild(PathElement.pathElement(CommonAttributes.REMOTE_CONNECTOR, connectorName))) {
                final ModelNode config = context.readResourceFromRoot(subsystemAddress.append(PathElement.pathElement(CommonAttributes.REMOTE_CONNECTOR, connectorName)), true).getModel();
                final Map<String, Object> parameters = getParameters(context, config, CONNECTORS_KEYS_MAP);
                ModelNode socketBinding = GenericTransportDefinition.SOCKET_BINDING.resolveModelAttribute(context, config);
                if (socketBinding.isDefined()) {
                    // uses the parameters to pass the socket binding name that will be read in ActiveMQServerService.start()
                    parameters.put(GenericTransportDefinition.SOCKET_BINDING.getName(), socketBinding.asString());
                    bindings.add(socketBinding.asString());
                }
                if(!config.hasDefined(FACTORY_CLASS.getName())) {
                    config.get(FACTORY_CLASS.getName()).set(NettyConnectorFactory.class.getName());
                }
                final String clazz = FACTORY_CLASS.resolveModelAttribute(context, config).asString();
                connectors.add(new TransportConfiguration(clazz, parameters, connectorName));
            }
            if (subsystemResource.hasChild(PathElement.pathElement(CommonAttributes.IN_VM_CONNECTOR, connectorName))) {
                final ModelNode config = context.readResourceFromRoot(subsystemAddress.append(PathElement.pathElement(CommonAttributes.IN_VM_CONNECTOR, connectorName)), true).getModel();
                final Map<String, Object> parameters = getParameters(context, config, CONNECTORS_KEYS_MAP);
                parameters.put(CONNECTORS_KEYS_MAP.get(InVMTransportDefinition.SERVER_ID.getName()), InVMTransportDefinition.SERVER_ID.resolveModelAttribute(context, config).asInt());
                connectors.add(new TransportConfiguration(InVMConnectorFactory.class.getName(), parameters, connectorName));
            }
            if (subsystemResource.hasChild(PathElement.pathElement(CommonAttributes.HTTP_CONNECTOR, connectorName))) {
                final ModelNode config = context.readResourceFromRoot(subsystemAddress.append(PathElement.pathElement(CommonAttributes.HTTP_CONNECTOR, connectorName)), true).getModel();
                final Map<String, Object> parameters = getParameters(context, config, CONNECTORS_KEYS_MAP);

                final String binding = HTTPConnectorDefinition.SOCKET_BINDING.resolveModelAttribute(context, config).asString();
                // ARTEMIS-803 Artemis knows that is must not offset the HTTP port when it is used by colocated backups
                parameters.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
                parameters.put(TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME, HTTPConnectorDefinition.ENDPOINT.resolveModelAttribute(context, config).asString());
                // uses the parameters to pass the socket binding name that will be read in ActiveMQServerService.start()
                parameters.put(HTTPConnectorDefinition.SOCKET_BINDING.getName(), binding);
                bindings.add(binding);
                ModelNode serverNameModelNode = HTTPConnectorDefinition.SERVER_NAME.resolveModelAttribute(context, config);
                if( serverNameModelNode.isDefined()) {
                    parameters.put(ACTIVEMQ_SERVER_NAME, serverNameModelNode.asString());
                }
                connectors.add(new TransportConfiguration(NettyConnectorFactory.class.getName(), parameters, connectorName));
            }
        }
        return connectors.toArray(new TransportConfiguration[connectors.size()]);
    }

    /**
     * Determines whether a socket-binding with the given name corresponds to a (local or remote) outbound-socket-binding
     * or a socket-binding.
     *
     * If no socket-binding or outbound-socket-binding resources matches, throw an OperationFailedException.
     */
//    public static boolean isOutBoundSocketBinding(OperationContext context, String name) throws OperationFailedException {
//        Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
//        for (Resource.ResourceEntry resource : root.getChildren(ModelDescriptionConstants.SOCKET_BINDING_GROUP)) {
//            if (resource.getChildrenNames(ModelDescriptionConstants.SOCKET_BINDING).contains(name)) {
//                return false;
//            } else if (resource.getChildrenNames(ModelDescriptionConstants.LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING).contains(name)
//                    || resource.getChildrenNames(ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING).contains(name))
//                return true;
//        }
//        throw MessagingLogger.ROOT_LOGGER.noSocketBinding(name);
//    }
    public static Map<String, Boolean> listOutBoundSocketBinding(OperationContext context, Collection<String> names) throws OperationFailedException {
        Map<String, Boolean> result = new HashMap<>();
        Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
        Set<String> groups = root.getChildrenNames(ModelDescriptionConstants.SOCKET_BINDING_GROUP);
        for (String groupName : groups) {
            Resource socketBindingGroup = context.readResourceFromRoot(PathAddress.pathAddress(ModelDescriptionConstants.SOCKET_BINDING_GROUP, groupName));
            for (String name : names) {
                if (socketBindingGroup.getChildrenNames(ModelDescriptionConstants.SOCKET_BINDING).contains(name)) {
                    result.put(name, Boolean.FALSE);
                } else if (socketBindingGroup.getChildrenNames(ModelDescriptionConstants.LOCAL_DESTINATION_OUTBOUND_SOCKET_BINDING).contains(name)
                        || socketBindingGroup.getChildrenNames(ModelDescriptionConstants.REMOTE_DESTINATION_OUTBOUND_SOCKET_BINDING).contains(name)) {
                    result.put(name, Boolean.TRUE);
                }
            }
        }
        if (result.size() != names.size()) {
            for(String name : names) {
                if(!result.containsKey(name)) {
                    throw MessagingLogger.ROOT_LOGGER.noSocketBinding(name);
                }
            }
        }
        return result;
    }
    public static void processConnectorBindings(Collection<TransportConfiguration> connectors,
            Map<String, Supplier<SocketBinding>> socketBindings,
            Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings) throws StartException {
        if (connectors != null) {
            for (TransportConfiguration tc : connectors) {
                // If there is a socket binding set the HOST/PORT values
                Object socketRef = tc.getParams().remove(SOCKET_REF);
                if (socketRef != null) {
                    String name = socketRef.toString();
                    String host;
                    int port;
                    if (!outboundSocketBindings.containsKey(name)) {
                        final SocketBinding socketBinding = socketBindings.get(name).get();
                        if (socketBinding == null) {
                            throw MessagingLogger.ROOT_LOGGER.failedToFindConnectorSocketBinding(tc.getName());
                        }
                        if (socketBinding.getClientMappings() != null && !socketBinding.getClientMappings().isEmpty()) {
                            // At the moment ActiveMQ doesn't allow to select mapping based on client's network.
                            // Instead the first client-mapping element will always be used - see WFLY-8432
                            ClientMapping clientMapping = socketBinding.getClientMappings().get(0);
                            host = NetworkUtils.canonize(clientMapping.getDestinationAddress());
                            port = clientMapping.getDestinationPort();

                            if (socketBinding.getClientMappings().size() > 1) {
                                MessagingLogger.ROOT_LOGGER.multipleClientMappingsFound(socketBinding.getName(), tc.getName(), host, port);
                            }
                        } else {
                            InetSocketAddress sa = socketBinding.getSocketAddress();
                            port = sa.getPort();
                            // resolve the host name of the address only if a loopback address has been set
                            if (sa.getAddress().isLoopbackAddress()) {
                                host = NetworkUtils.canonize(sa.getAddress().getHostName());
                            } else {
                                host = NetworkUtils.canonize(sa.getAddress().getHostAddress());
                            }
                        }
                    } else {
                        OutboundSocketBinding binding = outboundSocketBindings.get(name).get();
                        port = binding.getDestinationPort();
                        host = NetworkUtils.canonize(binding.getUnresolvedDestinationAddress());
                        if (binding.getSourceAddress() != null) {
                            tc.getParams().put(TransportConstants.LOCAL_ADDRESS_PROP_NAME,
                                    NetworkUtils.canonize(binding.getSourceAddress().getHostAddress()));
                        }
                        if (binding.getSourcePort() != null) {
                            // Use absolute port to account for source port offset/fixation
                            tc.getParams().put(TransportConstants.LOCAL_PORT_PROP_NAME, binding.getAbsoluteSourcePort());
                        }
                    }

                    tc.getParams().put(HOST, host);
                    tc.getParams().put(PORT, port);
                }
            }
        }
    }
}
