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
package org.jboss.as.clustering.jgroups;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jgroups.Channel;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestCorrelator.Header;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.fork.UnknownForkHandler;
import org.jgroups.protocols.FORK;
import org.jgroups.stack.ProtocolStack;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.TransportConfiguration;

/**
 * Factory for creating fork-able channels.
 * @author Paul Ferraro
 */
public class JChannelFactory implements ChannelFactory {

    static final ByteBuffer UNKNOWN_FORK_RESPONSE = ByteBuffer.allocate(0);

    private final ProtocolStackConfiguration configuration;

    public JChannelFactory(ProtocolStackConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ProtocolStackConfiguration getProtocolStackConfiguration() {
        return this.configuration;
    }

    @Override
    public Channel createChannel(String id) throws Exception {
        JChannel channel = new JChannel(false);
        ProtocolStack stack = new ProtocolStack();
        channel.setProtocolStack(stack);
        stack.addProtocols(Stream.of(
                Stream.of(this.configuration.getTransport()),
                this.configuration.getProtocols().stream(),
                Stream.of(this.configuration.getRelay())
        ).flatMap(Function.identity()).filter(Objects::nonNull).map(pc -> pc.createProtocol(this.configuration)).collect(Collectors.toList()));

        // Add implicit FORK to the top of the stack
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
                Header header = (Header) message.getHeader(this.id);
                // If this is a request expecting a response, don't leave the requester hanging - send an identifiable response on which it can filter
                if ((header != null) && (header.type == Header.REQ) && header.rspExpected()) {
                    Message response = message.makeReply().setFlag(message.getFlags()).clearFlag(Message.Flag.RSVP, Message.Flag.SCOPED);

                    response.putHeader(FORK.ID, message.getHeader(FORK.ID));
                    response.putHeader(this.id, new Header(Header.RSP, header.req_id, header.corrId));
                    response.setBuffer(UNKNOWN_FORK_RESPONSE.array());

                    channel.down(new Event(Event.MSG, response));
                }
                return null;
            }
        });
        stack.addProtocol(fork);
        stack.init();

        channel.setName(this.configuration.getNodeName());

        TransportConfiguration.Topology topology = this.configuration.getTransport().getTopology();
        if (topology != null) {
            channel.addAddressGenerator(new TopologyAddressGenerator(topology));
        }

        return channel;
    }

    @Override
    public boolean isUnknownForkResponse(ByteBuffer response) {
        return UNKNOWN_FORK_RESPONSE.equals(response);
    }
}
