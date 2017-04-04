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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

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
}
