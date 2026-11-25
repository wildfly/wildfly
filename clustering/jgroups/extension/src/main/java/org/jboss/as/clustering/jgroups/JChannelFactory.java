/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jgroups.EmptyMessage;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestCorrelator.Header;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.fork.UnknownForkHandler;
import org.jgroups.protocols.FORK;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.PhysicalAddressCache;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ChannelFactoryConfiguration;
import org.wildfly.clustering.jgroups.spi.TLSConfiguration;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Factory for creating fork-able channels.
 * @author Paul Ferraro
 */
public class JChannelFactory implements ChannelFactory {

    private final ChannelFactoryConfiguration configuration;

    public JChannelFactory(ChannelFactoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ChannelFactoryConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public JChannel createChannel(String id) throws Exception {
        // Transport always resides at the bottom of the stack
        // Add RELAY2 to the top of the stack, if defined
        List<ProtocolConfiguration<? extends Protocol>> configurations = Stream.concat(Stream.concat(Stream.of(this.configuration.getTransport()), this.configuration.getProtocols().stream()), this.configuration.getRelay().map(Stream::of).orElse(Stream.empty())).toList();

        if (configurations.stream().noneMatch(ProtocolConfiguration::providesConfidentiality)) {
            JGroupsLogger.CONFIG_LOGGER.allowsPublicMessages(id);
        }
        if (configurations.stream().noneMatch(ProtocolConfiguration::providesAuthentication)) {
            JGroupsLogger.CONFIG_LOGGER.allowsUnauthenticatedMembers(id);
        }

        FORK fork = new FORK();
        fork.enableStats(this.configuration.isStatisticsEnabled());
        fork.setUnknownForkHandler(new UnknownForkHandler() {
            private final short id = ClassConfigurator.getProtocolId(RequestCorrelator.class);

            @Override
            public Object handleUnknownForkStack(Message message, String forkStackId) {
                return this.handle(message);
            }

            @Override
            public Object handleUnknownForkChannel(Message message, String forkChannelId) {
                return this.handle(message);
            }

            private Object handle(Message message) {
                Header header = message.getHeader(this.id);
                // If this is a request expecting a response, don't leave the requester hanging - send an identifiable response on which it can filter
                if ((header != null) && (header.type == Header.REQ) && header.rspExpected()) {
                    Message response = new EmptyMessage(message.src()).setFlag(message.getFlags(), false).clearFlag(Message.Flag.RSVP);
                    if (message.getDest() != null) {
                        response.src(message.getDest());
                    }

                    response.putHeader(FORK.ID, message.getHeader(FORK.ID));
                    response.putHeader(this.id, new Header(Header.RSP, header.req_id, header.corrId));

                    fork.getProtocolStack().getChannel().down(response);
                }
                return null;
            }
        });

        Map<String, SocketBinding> bindings = new HashMap<>();
        List<Protocol> protocols = new ArrayList<>(configurations.size() + 1);
        for (ProtocolConfiguration<? extends Protocol> configuration : configurations) {
            protocols.add(configuration.createProtocol(this.configuration));
            bindings.putAll(configuration.getSocketBindings());
        }
        // Add implicit FORK to the top of the stack
        protocols.add(fork);

        // Override the SocketFactory of the transport
        TP transport = (TP) protocols.get(0);
        SocketBindingManager manager = this.configuration.getTransport().getSocketBinding().getSocketBindings();
        Optional<TLSConfiguration> tls = this.configuration.getTransport().getTLSConfiguration();

        transport.setSocketFactory(new ManagedSocketFactory(new ManagedSocketFactory.Configuration() {
            @Override
            public Map<String, SocketBinding> getSocketBindings() {
                return Collections.unmodifiableMap(bindings);
            }

            @Override
            public SocketBindingManager getSocketBindingManager() {
                return manager;
            }

            @Override
            public SSLContext getClientSSLContext() {
                return tls.map(TLSConfiguration::getClientSSLContext).orElse(null);
            }

            @Override
            public SSLContext getServerSSLContext() {
                return tls.map(TLSConfiguration::getServerSSLContext).orElse(null);
            }
        }));

        JChannel channel = createChannel(protocols);

        channel.setName(this.configuration.getMemberName());
        // Populate cache of physical addresses
        channel.addChannelListener(PhysicalAddressCache.INSTANCE);

        return channel;
    }

    // TODO Remove this once DNS_PING is configurable via an explicit DNSResolver
    private static JChannel createChannel(List<Protocol> protocols) throws Exception {
        // DNS_PING current loads its InitialContextFactory via the TCCL
        ClassLoader loader = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(JChannel.class);
            return new JChannel(protocols);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader);
        }
    }

    @Override
    public boolean isUnknownForkResponse(Message response) {
        return !response.hasPayload();
    }
}
