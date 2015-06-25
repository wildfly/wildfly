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
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CORE;

import java.io.IOException;

import io.netty.channel.socket.SocketChannel;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ListenerRegistry;
import io.undertow.server.handlers.ChannelUpgradeHandler;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptor;
import org.apache.activemq.artemis.core.remoting.server.RemotingService;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.remoting.HttpListenerRegistryService;
import org.jboss.as.remoting.SimpleHttpUpgradeHandshake;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.xnio.ChannelListener;
import org.xnio.StreamConnection;
import org.xnio.netty.transport.WrappingXnioSocketChannel;

/**
 * Service that handles HTTP upgrade for ActiveMQ remoting protocol.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class HTTPUpgradeService implements Service<HTTPUpgradeService> {

    public static final ServiceName HTTP_UPGRADE_REGISTRY = ServiceName.JBOSS.append("http-upgrade-registry");
    public static final ServiceName UPGRADE_SERVICE_NAME = MessagingServices.JBOSS_MESSAGING_ACTIVEMQ.append("http-upgrade-service");

    private final String activeMQServerName;
    private final String acceptorName;
    private final String httpListenerName;
    private InjectedValue<ChannelUpgradeHandler> injectedRegistry = new InjectedValue<>();
    private InjectedValue<ListenerRegistry> listenerRegistry = new InjectedValue<>();

    private ListenerRegistry.HttpUpgradeMetadata httpUpgradeMetadata;

    public HTTPUpgradeService(String activeMQServerName, String acceptorName, String httpListenerName) {
        this.activeMQServerName = activeMQServerName;
        this.acceptorName = acceptorName;
        this.httpListenerName = httpListenerName;
    }

    public static void installService(final ServiceTarget serviceTarget, String activeMQServerName, final String acceptorName, final String httpListenerName) {

        final HTTPUpgradeService service = new HTTPUpgradeService(activeMQServerName, acceptorName, httpListenerName);

        serviceTarget.addService(UPGRADE_SERVICE_NAME.append(acceptorName), service)
                .addDependency(HTTP_UPGRADE_REGISTRY.append(httpListenerName), ChannelUpgradeHandler.class, service.injectedRegistry)
                .addDependency(HttpListenerRegistryService.SERVICE_NAME, ListenerRegistry.class, service.listenerRegistry)
                .addDependency(ActiveMQActivationService.getServiceName(MessagingServices.getActiveMQServiceName(activeMQServerName)))
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        ListenerRegistry.Listener listenerInfo = listenerRegistry.getValue().getListener(httpListenerName);
        assert listenerInfo != null;
        httpUpgradeMetadata = new ListenerRegistry.HttpUpgradeMetadata(ACTIVEMQ_REMOTING, CORE);
        listenerInfo.addHttpUpgradeMetadata(httpUpgradeMetadata);

        MessagingLogger.ROOT_LOGGER.registeredHTTPUpgradeHandler(ACTIVEMQ_REMOTING, acceptorName);
        ServiceController<?> activeMQService = context.getController().getServiceContainer().getService(MessagingServices.getActiveMQServiceName(activeMQServerName));
        ActiveMQServer activeMQServer = ActiveMQServer.class.cast(activeMQService.getValue());

        injectedRegistry.getValue().addProtocol(ACTIVEMQ_REMOTING,
                switchToActiveMQCoreProtocol(activeMQServer, acceptorName),
                new SimpleHttpUpgradeHandshake(MAGIC_NUMBER, SEC_ACTIVEMQ_REMOTING_KEY, SEC_ACTIVEMQ_REMOTING_ACCEPT) {
                    /**
                     * override the default upgrade handshake to take into account the {@code TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME} header
                     * to select the correct acceptors among all that are configured in ActiveMQ.
                     *
                     * If the request does not have this header, the first acceptor will be used.
                     */
                    @Override
                    public boolean handleUpgrade(HttpServerExchange exchange) throws IOException {

                        if (super.handleUpgrade(exchange)) {
                            final String endpoint = exchange.getRequestHeaders().getFirst(HTTP_UPGRADE_ENDPOINT_PROP_NAME);
                            if (endpoint == null) {
                                return true;
                            } else {
                                return acceptorName.equals(endpoint);
                            }
                        } else {
                            return false;
                        }
                    }
                });
    }

    @Override
    public void stop(StopContext context) {
        listenerRegistry.getValue().getListener(httpListenerName).removeHttpUpgradeMetadata(httpUpgradeMetadata);
        httpUpgradeMetadata = null;
        injectedRegistry.getValue().removeProtocol(ACTIVEMQ_REMOTING);
    }

    @Override
    public HTTPUpgradeService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private static ChannelListener<StreamConnection> switchToActiveMQCoreProtocol(final ActiveMQServer activeMQServer, final String acceptorName) {
        return new ChannelListener<StreamConnection>() {
            @Override
            public void handleEvent(final StreamConnection connection) {
                MessagingLogger.ROOT_LOGGER.debugf("Switching to %s protocol for %s http-acceptor", ACTIVEMQ_REMOTING, acceptorName);
                SocketChannel channel = new WrappingXnioSocketChannel(connection);
                RemotingService remotingService = activeMQServer.getRemotingService();

                NettyAcceptor acceptor = (NettyAcceptor)remotingService.getAcceptor(acceptorName);
                acceptor.transfer(channel);
                connection.getSourceChannel().resumeReads();
            }
        };
    }

}
