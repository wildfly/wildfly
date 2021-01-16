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

package org.wildfly.extension.messaging.activemq;

import static org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnector.ACTIVEMQ_REMOTING;
import static org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnector.MAGIC_NUMBER;
import static org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnector.SEC_ACTIVEMQ_REMOTING_ACCEPT;
import static org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnector.SEC_ACTIVEMQ_REMOTING_KEY;
import static org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME;
import static org.hornetq.core.remoting.impl.netty.NettyConnector.HORNETQ_REMOTING;
import static org.hornetq.core.remoting.impl.netty.NettyConnector.SEC_HORNETQ_REMOTING_ACCEPT;
import static org.hornetq.core.remoting.impl.netty.NettyConnector.SEC_HORNETQ_REMOTING_KEY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CORE;
import static org.wildfly.extension.messaging.activemq.Capabilities.HTTP_LISTENER_REGISTRY_CAPABILITY_NAME;
import static org.wildfly.extension.messaging.activemq.Capabilities.HTTP_UPGRADE_REGISTRY_CAPABILITY_NAME;

import java.io.IOException;

import io.netty.channel.socket.SocketChannel;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.HttpUpgradeListener;
import io.undertow.server.ListenerRegistry;
import io.undertow.server.handlers.ChannelUpgradeHandler;
import io.undertow.server.handlers.HttpUpgradeHandshake;
import io.undertow.util.FlexBase64;
import io.undertow.util.HttpString;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptor;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.remoting.server.RemotingService;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.cluster.ClusterManager;
import org.apache.activemq.artemis.core.server.cluster.ha.HAManager;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.StreamConnection;
import org.xnio.netty.transport.WrappingXnioSocketChannel;

/**
 * Service that handles HTTP upgrade for ActiveMQ remoting protocol.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class HTTPUpgradeService implements Service<HTTPUpgradeService> {

    private final String activeMQServerName;
    private final String acceptorName;
    private final String httpListenerName;
    private final Supplier<ChannelUpgradeHandler> upgradeSupplier;
    private final Supplier<ListenerRegistry> listenerRegistrySupplier;

    // Keep a reference to the HttpUpgradeListener that is created when the service is started.
    // There are many HttpUpgradeListener fro the artemis-remoting protocol (one per http-acceptor, for each
    // activemq server) and only this httpUpgradeListener must be removed from the ChannelUpgradeHandler registry
    // when this service is stopped.
    private HttpUpgradeListener httpUpgradeListener;
    private ListenerRegistry.HttpUpgradeMetadata httpUpgradeMetadata;

    public HTTPUpgradeService(String activeMQServerName, String acceptorName, String httpListenerName, Supplier<ChannelUpgradeHandler> upgradeSupplier, Supplier<ListenerRegistry> listenerRegistrySupplier) {
        this.activeMQServerName = activeMQServerName;
        this.acceptorName = acceptorName;
        this.httpListenerName = httpListenerName;
        this.upgradeSupplier = upgradeSupplier;
        this.listenerRegistrySupplier = listenerRegistrySupplier;
    }

    @SuppressWarnings("unchecked")
    public static void installService(final CapabilityServiceTarget target, String activeMQServerName, final String acceptorName, final String httpListenerName) {
        final CapabilityServiceBuilder sb = target.addCapability(HTTPAcceptorDefinition.CAPABILITY);
        sb.provides(MessagingServices.getHttpUpgradeServiceName(activeMQServerName, acceptorName));
        Supplier<ChannelUpgradeHandler> upgradeSupplier = sb.requiresCapability(HTTP_UPGRADE_REGISTRY_CAPABILITY_NAME, ChannelUpgradeHandler.class, httpListenerName);
        Supplier<ListenerRegistry> listenerRegistrySupplier = sb.requiresCapability(HTTP_LISTENER_REGISTRY_CAPABILITY_NAME, ListenerRegistry.class);
        sb.requires(ActiveMQActivationService.getServiceName(MessagingServices.getActiveMQServiceName(activeMQServerName)));
        final HTTPUpgradeService service = new HTTPUpgradeService(activeMQServerName, acceptorName, httpListenerName, upgradeSupplier, listenerRegistrySupplier);
        sb.setInitialMode(ServiceController.Mode.PASSIVE);
        sb.setInstance(service);
        sb.install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        ListenerRegistry listenerRegistry = listenerRegistrySupplier.get();
        ListenerRegistry.Listener listenerInfo = listenerRegistry.getListener(httpListenerName);
        assert listenerInfo != null;
        httpUpgradeMetadata = new ListenerRegistry.HttpUpgradeMetadata(getProtocol(), CORE);
        listenerInfo.addHttpUpgradeMetadata(httpUpgradeMetadata);

        MessagingLogger.ROOT_LOGGER.registeredHTTPUpgradeHandler(ACTIVEMQ_REMOTING, acceptorName);
        ServiceController<?> activeMQService = context.getController().getServiceContainer().getService(MessagingServices.getActiveMQServiceName(activeMQServerName));
        ActiveMQServer activeMQServer = ActiveMQServer.class.cast(activeMQService.getValue());

        httpUpgradeListener = switchToMessagingProtocol(activeMQServer, acceptorName, getProtocol());
        upgradeSupplier.get().addProtocol(getProtocol(),
                httpUpgradeListener,
                new HttpUpgradeHandshake() {
            /**
             * override the default upgrade handshake to take into account the
             * {@code TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME} header
             * to select the correct acceptors among all that are configured in ActiveMQ.
             *
             * If the request does not have this header, the first acceptor will be used.
             */
            @Override
            public boolean handleUpgrade(HttpServerExchange exchange) throws IOException {
                String secretKey = exchange.getRequestHeaders().getFirst(getSecKeyHeader());
                if (secretKey == null) {
                    throw MessagingLogger.ROOT_LOGGER.upgradeRequestMissingKey();
                }
                ActiveMQServer server = selectServer(exchange, activeMQServer);
                if (server == null) {
                    return false;
                }
                // If ActiveMQ remoting service is stopped (eg during shutdown), refuse
                // the handshake so that the ActiveMQ client will detect the connection has failed
                RemotingService remotingService = server.getRemotingService();
                if (!server.isActive() || !remotingService.isStarted()) {
                    return false;
                }
                final String endpoint = exchange.getRequestHeaders().getFirst(getHttpUpgradeEndpointKey());
                if (endpoint == null || endpoint.equals(acceptorName)) {
                    String response = createExpectedResponse(MAGIC_NUMBER, secretKey);
                    exchange.getResponseHeaders().put(HttpString.tryFromString(getSecAcceptHeader()), response);
                    return true;
                }
                return false;
            }

            private String createExpectedResponse(final String magicNumber, final String secretKey) throws IOException {
                try {
                    final String concat = secretKey + magicNumber;
                    final MessageDigest digest = MessageDigest.getInstance("SHA1");

                    digest.update(concat.getBytes(StandardCharsets.UTF_8));
                    final byte[] bytes = digest.digest();
                    return FlexBase64.encodeString(bytes, false);
                } catch (NoSuchAlgorithmException e) {
                    throw new IOException(e);
                }
            }
        });
    }

    private static ActiveMQServer selectServer(HttpServerExchange exchange, ActiveMQServer rootServer) {
        String activemqServerName = exchange.getRequestHeaders().getFirst(TransportConstants.ACTIVEMQ_SERVER_NAME);
        if (activemqServerName == null) {
            return rootServer;
        }
        ClusterManager clusterManager = rootServer.getClusterManager();
        if (clusterManager != null) {
            HAManager haManager = clusterManager.getHAManager();
            if (haManager != null) {
                for (Map.Entry<String, ActiveMQServer> entry : haManager.getBackupServers().entrySet()) {
                    if (entry.getKey().equals(activemqServerName)) {
                        return entry.getValue();
                    }
                }
            }
        }

        if (activemqServerName.equals(rootServer.getConfiguration().getName())) {
            return rootServer;
        } else {
            return null;
        }
    }

    @Override
    public void stop(StopContext context) {
        listenerRegistrySupplier.get().getListener(httpListenerName).removeHttpUpgradeMetadata(httpUpgradeMetadata);
        httpUpgradeMetadata = null;
        upgradeSupplier.get().removeProtocol(getProtocol(), httpUpgradeListener);
        httpUpgradeListener = null;
    }

    @Override
    public HTTPUpgradeService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private static HttpUpgradeListener switchToMessagingProtocol(final ActiveMQServer activemqServer, final String acceptorName, final String protocolName) {
        return new HttpUpgradeListener() {
            @Override
            public void handleUpgrade(StreamConnection streamConnection, HttpServerExchange exchange) {
                ChannelListener<StreamConnection> listener = new ChannelListener<StreamConnection>() {
                    @Override
                    public void handleEvent(StreamConnection connection) {
                        MessagingLogger.ROOT_LOGGER.debugf("Switching to %s protocol for %s http-acceptor", protocolName, acceptorName);
                        ActiveMQServer server = selectServer(exchange, activemqServer);
                        RemotingService remotingService = server.getRemotingService();
                        if (!server.isActive() || !remotingService.isStarted()) {
                            // ActiveMQ does not accept connection
                            IoUtils.safeClose(connection);
                            return;
                        }
                        NettyAcceptor acceptor = (NettyAcceptor) remotingService.getAcceptor(acceptorName);
                        SocketChannel channel = new WrappingXnioSocketChannel(connection);
                        try {
                            acceptor.transfer(channel);
                            connection.getSourceChannel().resumeReads();
                        } catch (IllegalStateException e) {
                            IoUtils.safeClose(connection);
                        }
                    }
                };
                ChannelListeners.invokeChannelListener(streamConnection, listener);
            }
        };
    }

    protected String getProtocol() {
        return ACTIVEMQ_REMOTING;
    }

    protected String getSecKeyHeader() {
        return SEC_ACTIVEMQ_REMOTING_KEY;
    }

    protected String getSecAcceptHeader() {
        return SEC_ACTIVEMQ_REMOTING_ACCEPT;
    }

    protected String getHttpUpgradeEndpointKey() {
        return HTTP_UPGRADE_ENDPOINT_PROP_NAME;
    }


    /**
     * Service to handle HTTP upgrade for legacy (HornetQ) clients.
     *
     * Legacy clients use different protocol and security key and accept headers during the HTTP Upgrade handshake.
     */
    static class LegacyHttpUpgradeService extends HTTPUpgradeService {

        public static void installService(final OperationContext context, String activeMQServerName, final String acceptorName, final String httpListenerName) {
            final CapabilityServiceBuilder sb = context.getCapabilityServiceTarget().addCapability(RuntimeCapability.Builder.of(MessagingServices.getLegacyHttpUpgradeServiceName(activeMQServerName, acceptorName).getCanonicalName(), false, LegacyHttpUpgradeService.class).build());
            Supplier<ChannelUpgradeHandler> upgradeSupplier = sb.requiresCapability(HTTP_UPGRADE_REGISTRY_CAPABILITY_NAME, ChannelUpgradeHandler.class, httpListenerName);
            Supplier<ListenerRegistry> listenerRegistrySupplier = sb.requiresCapability(HTTP_LISTENER_REGISTRY_CAPABILITY_NAME, ListenerRegistry.class);
            sb.requires(ActiveMQActivationService.getServiceName(MessagingServices.getActiveMQServiceName(activeMQServerName)));
            sb.setInitialMode(ServiceController.Mode.PASSIVE);
            final LegacyHttpUpgradeService service = new LegacyHttpUpgradeService(activeMQServerName, acceptorName, httpListenerName, upgradeSupplier, listenerRegistrySupplier);
            sb.setInstance(service);
            sb.install();
        }

        private LegacyHttpUpgradeService(String activeMQServerName, String acceptorName, String httpListenerName, Supplier<ChannelUpgradeHandler> upgradeSupplier, Supplier<ListenerRegistry> listenerRegistrySupplier) {
            super(activeMQServerName, acceptorName, httpListenerName, upgradeSupplier, listenerRegistrySupplier);
        }

        @Override
        protected String getProtocol() {
            return HORNETQ_REMOTING;
        }

        @Override
        protected String getHttpUpgradeEndpointKey() {
            return "http-upgrade-endpoint";
        }

        @Override
        protected String getSecKeyHeader() {
            return SEC_HORNETQ_REMOTING_KEY;
        }

        @Override
        protected String getSecAcceptHeader() {
            return SEC_HORNETQ_REMOTING_ACCEPT;
        }
    }

}
