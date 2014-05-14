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

package org.jboss.as.messaging;

import static org.hornetq.core.remoting.impl.netty.NettyConnector.HORNETQ_REMOTING;
import static org.hornetq.core.remoting.impl.netty.NettyConnector.MAGIC_NUMBER;
import static org.hornetq.core.remoting.impl.netty.NettyConnector.SEC_HORNETQ_REMOTING_ACCEPT;
import static org.hornetq.core.remoting.impl.netty.NettyConnector.SEC_HORNETQ_REMOTING_KEY;
import static org.hornetq.core.remoting.impl.netty.TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME;
import static org.jboss.as.messaging.CommonAttributes.CORE;
import static org.jboss.as.messaging.logging.MessagingLogger.MESSAGING_LOGGER;

import java.io.IOException;
import java.util.List;

import io.netty.channel.socket.SocketChannel;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ListenerRegistry;
import io.undertow.server.handlers.ChannelUpgradeHandler;
import org.hornetq.core.remoting.impl.netty.NettyAcceptor;
import org.hornetq.core.remoting.server.RemotingService;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.remoting.HttpListenerRegistryService;
import org.jboss.as.remoting.SimpleHttpUpgradeHandshake;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.xnio.ChannelListener;
import org.xnio.StreamConnection;
import org.xnio.netty.transport.WrappingXnioSocketChannel;

/**
 * Service that handles HTTP upgrade for HornetQ remoting protocol.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class HTTPUpgradeService implements Service<HTTPUpgradeService> {

    public static final ServiceName HTTP_UPGRADE_REGISTRY = ServiceName.JBOSS.append("http-upgrade-registry");
    public static final ServiceName UPGRADE_SERVICE_NAME = MessagingServices.JBOSS_MESSAGING.append("http-upgrade-service");

    private final String hornetQServerName;
    private final String acceptorName;
    private final String httpListenerName;
    private InjectedValue<ChannelUpgradeHandler> injectedRegistry = new InjectedValue<>();
    private InjectedValue<ListenerRegistry> listenerRegistry = new InjectedValue<>();

    private ListenerRegistry.HttpUpgradeMetadata httpUpgradeMetadata;

    public HTTPUpgradeService(String hornetQServerName, String acceptorName, String httpListenerName) {
        this.hornetQServerName = hornetQServerName;
        this.acceptorName = acceptorName;
        this.httpListenerName = httpListenerName;
    }

    public static void installService(final ServiceTarget serviceTarget, String hornetQServerName, final String acceptorName, final String httpListenerName, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        final HTTPUpgradeService service = new HTTPUpgradeService(hornetQServerName, acceptorName, httpListenerName);

        ServiceBuilder<HTTPUpgradeService> builder = serviceTarget.addService(UPGRADE_SERVICE_NAME.append(acceptorName), service)
                .addDependency(HTTP_UPGRADE_REGISTRY.append(httpListenerName), ChannelUpgradeHandler.class, service.injectedRegistry)
                .addDependency(HttpListenerRegistryService.SERVICE_NAME, ListenerRegistry.class, service.listenerRegistry)
                .addDependency(HornetQActivationService.getHornetQActivationServiceName(MessagingServices.getHornetQServiceName(hornetQServerName)));

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        builder.setInitialMode(ServiceController.Mode.PASSIVE);

        ServiceController<HTTPUpgradeService> controller = builder.install();
        if(newControllers != null) {
            newControllers.add(controller);
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        ListenerRegistry.Listener listenerInfo = listenerRegistry.getValue().getListener(httpListenerName);
        assert listenerInfo != null;
        httpUpgradeMetadata = new ListenerRegistry.HttpUpgradeMetadata(HORNETQ_REMOTING, CORE);
        listenerInfo.addHttpUpgradeMetadata(httpUpgradeMetadata);

        MESSAGING_LOGGER.registeredHTTPUpgradeHandler(HORNETQ_REMOTING, acceptorName);
        ServiceController<?> hornetqService = context.getController().getServiceContainer().getService(MessagingServices.getHornetQServiceName(hornetQServerName));
        HornetQServer hornetQServer = HornetQServer.class.cast(hornetqService.getValue());

        injectedRegistry.getValue().addProtocol(HORNETQ_REMOTING,
                switchToHornetQProtocol(hornetQServer, acceptorName),
                new SimpleHttpUpgradeHandshake(MAGIC_NUMBER, SEC_HORNETQ_REMOTING_KEY, SEC_HORNETQ_REMOTING_ACCEPT) {
                    /**
                     * override the default upgrade handshake to take into account the {@code TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME} header
                     * to select the correct acceptors among all that are configured in HornetQ.
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
        injectedRegistry.getValue().removeProtocol(HORNETQ_REMOTING);
    }

    @Override
    public HTTPUpgradeService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private static ChannelListener<StreamConnection> switchToHornetQProtocol(final HornetQServer hornetqServer, final String acceptorName) {
        return new ChannelListener<StreamConnection>() {
            @Override
            public void handleEvent(final StreamConnection connection) {
                MESSAGING_LOGGER.debugf("Switching to %s protocol for %s http-acceptor", HORNETQ_REMOTING, acceptorName);
                SocketChannel channel = new WrappingXnioSocketChannel(connection);
                RemotingService remotingService = hornetqServer.getRemotingService();

                NettyAcceptor acceptor = (NettyAcceptor)remotingService.getAcceptor(acceptorName);
                acceptor.transfer(channel);
                connection.getSourceChannel().resumeReads();
            }
        };
    }

}
