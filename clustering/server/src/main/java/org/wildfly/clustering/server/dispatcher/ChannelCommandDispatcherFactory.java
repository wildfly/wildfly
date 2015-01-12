/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.dispatcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.Unmarshaller;
import org.jgroups.Address;
import org.jgroups.MembershipListener;
import org.jgroups.MergeView;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.marshalling.MarshallingContext;
import org.wildfly.clustering.server.group.JGroupsNodeFactory;

/**
 * {@link MessageDispatcher} based {@link CommandDispatcherFactory}.
 * This factory can produce multiple {@link CommandDispatcher} instances,
 * all of which will share the same {@link MessageDispatcher} instance.
 * @author Paul Ferraro
 */
public class ChannelCommandDispatcherFactory implements CommandDispatcherFactory, RequestHandler, AutoCloseable, Group, MembershipListener {

    final Map<Object, AtomicReference<Object>> contexts = new ConcurrentHashMap<>();
    final MarshallingContext marshallingContext;

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicReference<View> view = new AtomicReference<>();
    private final MessageDispatcher dispatcher;
    private final JGroupsNodeFactory nodeFactory;
    private final long timeout;

    public ChannelCommandDispatcherFactory(ChannelCommandDispatcherFactoryConfiguration config) {
        this.nodeFactory = config.getNodeFactory();
        this.marshallingContext = config.getMarshallingContext();
        this.timeout = config.getTimeout();
        final RpcDispatcher.Marshaller marshaller = new CommandResponseMarshaller(this.marshallingContext);
        this.dispatcher = new MessageDispatcher() {
            @Override
            protected RequestCorrelator createRequestCorrelator(Protocol transport, RequestHandler handler, Address localAddr) {
                RequestCorrelator correlator = super.createRequestCorrelator(transport, handler, localAddr);
                correlator.setMarshaller(marshaller);
                return correlator;
            }
        };
        this.dispatcher.setChannel(config.getChannel());
        this.dispatcher.setRequestHandler(this);
        this.dispatcher.setMembershipListener(this);
        this.dispatcher.start();
    }

    @Override
    public void close() {
        this.dispatcher.stop();
    }

    @Override
    public Object handle(Message message) throws Exception {
        try (InputStream input = new ByteArrayInputStream(message.getRawBuffer(), message.getOffset(), message.getLength())) {
            int version = input.read();
            try (Unmarshaller unmarshaller = this.marshallingContext.createUnmarshaller(version)) {
                unmarshaller.start(Marshalling.createByteInput(input));
                Object clientId = unmarshaller.readObject();
                @SuppressWarnings("unchecked")
                Command<Object, Object> command = (Command<Object, Object>) unmarshaller.readObject();
                AtomicReference<Object> context = this.contexts.get(clientId);
                if (context == null) return new NoSuchService();
                return command.execute(context.get());
            }
        }
    }

    @Override
    public Group getGroup() {
        return this;
    }

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(final Object id, C context) {
        final int version = this.marshallingContext.getCurrentVersion();
        CommandMarshaller<C> marshaller = new CommandMarshaller<C>() {
            @Override
            public <R> byte[] marshal(Command<R, C> command) throws IOException {
                try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    output.write(version);
                    try (Marshaller marshaller = ChannelCommandDispatcherFactory.this.marshallingContext.createMarshaller(version)) {
                        marshaller.start(Marshalling.createByteOutput(output));
                        marshaller.writeObject(id);
                        marshaller.writeObject(command);
                        marshaller.flush();
                    }
                    return output.toByteArray();
                }
            }
        };
        this.contexts.put(id, new AtomicReference<Object>(context));
        final CommandDispatcher<C> localDispatcher = new LocalCommandDispatcher<>(this.getLocalNode(), context);
        return new ChannelCommandDispatcher<C>(this.dispatcher, marshaller, this.nodeFactory, this.timeout, localDispatcher) {
            @Override
            public void close() {
                localDispatcher.close();
                ChannelCommandDispatcherFactory.this.contexts.remove(id);
            }
        };
    }

    @Override
    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public String getName() {
        return this.dispatcher.getChannel().getClusterName();
    }

    @Override
    public boolean isCoordinator() {
        return this.dispatcher.getChannel().getAddress().equals(this.getCoordinatorAddress());
    }

    @Override
    public Node getLocalNode() {
        return this.nodeFactory.createNode(this.dispatcher.getChannel().getAddress());
    }

    @Override
    public Node getCoordinatorNode() {
        return this.nodeFactory.createNode(this.getCoordinatorAddress());
    }

    @Override
    public List<Node> getNodes() {
        return getNodes(this.view.get());
    }

    private Address getCoordinatorAddress() {
        List<Address> members = this.view.get().getMembers();
        return !members.isEmpty() ? members.get(0) : null;
    }

    private List<Node> getNodes(View view) {
        return (view != null) ? this.getNodes(view.getMembers()) : Collections.<Node>emptyList();
    }

    private List<Node> getNodes(List<Address> addresses) {
        List<Node> nodes = new ArrayList<>(addresses.size());
        for (Address address: addresses) {
            nodes.add(this.nodeFactory.createNode(address));
        }
        return nodes;
    }

    @Override
    public void viewAccepted(View view) {
        View oldView = this.view.getAndSet(view);
        List<Node> oldNodes = this.getNodes(oldView);
        List<Node> newNodes = this.getNodes(view);

        List<Address> leftMembers = View.leftMembers(oldView, view);
        if (leftMembers != null) {
            this.nodeFactory.invalidate(leftMembers);
        }

        for (Listener listener: this.listeners) {
            listener.membershipChanged(oldNodes, newNodes, view instanceof MergeView);
        }
    }

    @Override
    public void suspect(Address member) {
    }

    @Override
    public void block() {
    }

    @Override
    public void unblock() {
    }
}
