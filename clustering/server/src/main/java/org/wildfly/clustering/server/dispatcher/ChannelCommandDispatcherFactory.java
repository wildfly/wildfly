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
import java.io.DataInputStream;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.threads.JBossThreadFactory;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.MembershipListener;
import org.jgroups.MergeView;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.spi.IndexExternalizer;
import org.wildfly.clustering.server.group.JGroupsNodeFactory;
import org.wildfly.clustering.service.concurrent.ClassLoaderThreadFactory;
import org.wildfly.clustering.service.concurrent.ServiceExecutor;
import org.wildfly.clustering.service.concurrent.StampedLockServiceExecutor;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link MessageDispatcher} based {@link CommandDispatcherFactory}.
 * This factory can produce multiple {@link CommandDispatcher} instances,
 * all of which will share the same {@link MessageDispatcher} instance.
 * @author Paul Ferraro
 */
public class ChannelCommandDispatcherFactory implements CommandDispatcherFactory, RequestHandler, AutoCloseable, Group, MembershipListener {

    private static ThreadFactory createThreadFactory(Class<?> targetClass) {
        PrivilegedAction<ThreadFactory> action = () -> new JBossThreadFactory(new ThreadGroup(targetClass.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null);
        return new ClassLoaderThreadFactory(WildFlySecurityManager.doUnchecked(action), targetClass.getClassLoader());
    }

    private final Map<Object, Optional<Object>> contexts = new ConcurrentHashMap<>();
    private final ServiceExecutor executor = new StampedLockServiceExecutor();
    private final Map<Listener, ExecutorService> listeners = new ConcurrentHashMap<>();
    private final AtomicReference<View> view = new AtomicReference<>();
    private final JGroupsNodeFactory nodeFactory;
    private final MarshallingContext marshallingContext;
    private final MessageDispatcher dispatcher;
    private final long timeout;

    public ChannelCommandDispatcherFactory(ChannelCommandDispatcherFactoryConfiguration config) {
        this.nodeFactory = config.getNodeFactory();
        this.marshallingContext = config.getMarshallingContext();
        this.timeout = config.getTimeout();
        this.dispatcher = new MessageDispatcher() {
            @Override
            protected RequestCorrelator createRequestCorrelator(Protocol transport, RequestHandler handler, Address localAddr) {
                RequestCorrelator correlator = super.createRequestCorrelator(transport, handler, localAddr);
                correlator.setMarshaller(new CommandResponseMarshaller(config));
                return correlator;
            }
        };
        Channel channel = config.getChannel();
        this.dispatcher.setChannel(channel);
        this.dispatcher.setRequestHandler(this);
        this.dispatcher.setMembershipListener(this);
        this.dispatcher.asyncDispatching(true).start();
        this.view.compareAndSet(null, channel.getView());
    }

    @Override
    public void close() {
        this.executor.close(() -> {
            this.dispatcher.stop();
            this.dispatcher.getChannel().setUpHandler(null);
            // Cleanup any stray listeners
            this.listeners.values().forEach(executor -> {
                PrivilegedAction<List<Runnable>> action = () -> executor.shutdownNow();
                WildFlySecurityManager.doUnchecked(action);
            });
            this.listeners.clear();
        });
    }

    @Override
    public Object handle(Message message) throws Exception {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message.getRawBuffer(), message.getOffset(), message.getLength()))) {
            int version = IndexExternalizer.VARIABLE.readData(input);
            try (Unmarshaller unmarshaller = this.marshallingContext.createUnmarshaller(version)) {
                unmarshaller.start(Marshalling.createByteInput(input));
                Object clientId = unmarshaller.readObject();
                Optional<Object> context = this.contexts.get(clientId);
                if (context == null) return NoSuchService.INSTANCE;
                @SuppressWarnings("unchecked")
                Command<Object, Object> command = (Command<Object, Object>) unmarshaller.readObject();
                Callable<Optional<Object>> task = new Callable<Optional<Object>>() {
                    @Override
                    public Optional<Object> call() throws Exception {
                        // Wrap in an Optional, since command execution might return null
                        return Optional.ofNullable(command.execute(context.orElse(null)));
                    }
                };
                return this.executor.execute(task).orElse(Optional.of(NoSuchService.INSTANCE)).orElse(null);
            }
        }
    }

    @Override
    public Group getGroup() {
        return this;
    }

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(Object id, C context) {
        this.contexts.put(id, Optional.ofNullable(context));
        CommandMarshaller<C> marshaller = new CommandDispatcherMarshaller<>(this.marshallingContext, id);
        CommandDispatcher<C> localDispatcher = new LocalCommandDispatcher<>(this.getLocalNode(), context);
        return new ChannelCommandDispatcher<>(this.dispatcher, marshaller, this.nodeFactory, this.timeout, localDispatcher, () -> {
            localDispatcher.close();
            this.contexts.remove(id);
        });
    }

    @Override
    public void addListener(Listener listener) {
        this.listeners.computeIfAbsent(listener, key -> Executors.newSingleThreadExecutor(createThreadFactory(listener.getClass())));
    }

    @Override
    public void removeListener(Listener listener) {
        ExecutorService executor = this.listeners.remove(listener);
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(this.timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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

    @Override
    public boolean isLocal() {
        return false;
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
        if (oldView != null) {
            List<Node> oldNodes = this.getNodes(oldView);
            List<Node> newNodes = this.getNodes(view);

            List<Address> leftMembers = View.leftMembers(oldView, view);
            if (leftMembers != null) {
                this.nodeFactory.invalidate(leftMembers);
            }

            for (Map.Entry<Listener, ExecutorService> entry : this.listeners.entrySet()) {
                try {
                    Listener listener = entry.getKey();
                    ExecutorService executor = entry.getValue();
                    executor.submit(() -> {
                        try {
                            listener.membershipChanged(oldNodes, newNodes, view instanceof MergeView);
                        } catch (Throwable e) {
                            ClusteringLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    // Executor was shutdown
                }
            }
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
