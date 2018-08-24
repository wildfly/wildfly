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
import java.net.InetSocketAddress;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.MergeView;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.Response;
import org.jgroups.stack.IpAddress;
import org.jgroups.util.NameCache;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.clustering.server.group.AddressableNode;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.service.concurrent.ClassLoaderThreadFactory;
import org.wildfly.clustering.service.concurrent.ServiceExecutor;
import org.wildfly.clustering.service.concurrent.StampedLockServiceExecutor;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link MessageDispatcher} based {@link CommandDispatcherFactory}.
 * This factory can produce multiple {@link CommandDispatcher} instances,
 * all of which will share the same {@link MessageDispatcher} instance.
 * @author Paul Ferraro
 */
public class ChannelCommandDispatcherFactory implements AutoCloseableCommandDispatcherFactory, RequestHandler, org.wildfly.clustering.server.group.Group<Address>, MembershipListener, Runnable {

    private static ThreadFactory createThreadFactory(Class<?> targetClass) {
        PrivilegedAction<ThreadFactory> action = () -> new ClassLoaderThreadFactory(new JBossThreadFactory(new ThreadGroup(targetClass.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null), targetClass.getClassLoader());
        return WildFlySecurityManager.doUnchecked(action);
    }

    private final ConcurrentMap<Address, Node> members = new ConcurrentHashMap<>();
    // Store execution context using an Optional so we can differentiate an unknown service from a known service with a null context
    private final Map<Object, Optional<Object>> contexts = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool(createThreadFactory(this.getClass()));
    private final ServiceExecutor executor = new StampedLockServiceExecutor();
    private final Map<GroupListener, ExecutorService> listeners = new ConcurrentHashMap<>();
    private final AtomicReference<View> view = new AtomicReference<>();
    private final MarshallingContext marshallingContext;
    private final MessageDispatcher dispatcher;
    private final Duration timeout;

    @SuppressWarnings("resource")
    public ChannelCommandDispatcherFactory(ChannelCommandDispatcherFactoryConfiguration config) {
        this.marshallingContext = config.getMarshallingContext();
        this.timeout = config.getTimeout();
        JChannel channel = config.getChannel();
        RequestCorrelator correlator = new RequestCorrelator(channel.getProtocolStack().getTransport(), this, channel.getAddress()).setMarshaller(new CommandResponseMarshaller(config));
        this.dispatcher = new MessageDispatcher()
                .setChannel(channel)
                .setRequestHandler(this)
                .setMembershipListener(this)
                .asyncDispatching(true)
                // Setting the request correlator starts the dispatcher
                .correlator(correlator)
                ;
        this.view.compareAndSet(null, channel.getView());
    }

    @Override
    public void run() {
        this.executorService.shutdownNow();
        try {
            this.executorService.awaitTermination(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.dispatcher.stop();
        this.dispatcher.getChannel().setUpHandler(null);
        // Cleanup any stray listeners
        for (ExecutorService executor : this.listeners.values()) {
            PrivilegedAction<List<Runnable>> action = () -> executor.shutdownNow();
            WildFlySecurityManager.doUnchecked(action);
        }
        this.listeners.clear();
    }

    @Override
    public void close() {
        this.executor.close(this);
    }

    @Override
    public Object handle(Message request) throws Exception {
        return this.read(request).call();
    }

    @Override
    public void handle(Message request, Response response) throws Exception {
        Callable<Object> task = this.read(request);
        try {
            this.executorService.submit(() -> {
                try {
                    response.send(task.call(), false);
                } catch (Exception e) {
                    response.send(e, true);
                }
            });
        } catch (RejectedExecutionException e) {
            response.send(NoSuchService.INSTANCE, false);
        }
    }

    private Callable<Object> read(Message message) throws Exception {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(message.getRawBuffer(), message.getOffset(), message.getLength()))) {
            int version = IndexSerializer.VARIABLE.readInt(input);
            try (Unmarshaller unmarshaller = this.marshallingContext.createUnmarshaller(version)) {
                unmarshaller.start(Marshalling.createByteInput(input));
                Object clientId = unmarshaller.readObject();
                Optional<Object> context = this.contexts.get(clientId);
                if (context == null) return () -> NoSuchService.INSTANCE;
                @SuppressWarnings("unchecked")
                Command<Object, Object> command = (Command<Object, Object>) unmarshaller.readObject();
                // Wrap execution result in an Optional, since command execution might return null
                ExceptionSupplier<Optional<Object>, Exception> task = () -> Optional.ofNullable(command.execute(context.orElse(null)));
                return () -> this.executor.execute(task).orElse(Optional.of(NoSuchService.INSTANCE)).orElse(null);
            }
        }
    }

    @Override
    public Group getGroup() {
        return this;
    }

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(Object id, C context) {
        if (this.contexts.putIfAbsent(id, Optional.ofNullable(context)) != null) {
            throw ClusteringServerLogger.ROOT_LOGGER.commandDispatcherAlreadyExists(id);
        }
        CommandMarshaller<C> marshaller = new CommandDispatcherMarshaller<>(this.marshallingContext, id);
        CommandDispatcher<C> localDispatcher = new LocalCommandDispatcher<>(this.getLocalMember(), context, this.executorService);
        return new ChannelCommandDispatcher<>(this.dispatcher, marshaller, this, this.timeout, localDispatcher, () -> {
            localDispatcher.close();
            this.contexts.remove(id);
        });
    }

    @Override
    public Registration register(GroupListener listener) {
        this.listeners.computeIfAbsent(listener, key -> Executors.newSingleThreadExecutor(createThreadFactory(listener.getClass())));
        return () -> this.unregister(listener);
    }

    private void unregister(GroupListener listener) {
        ExecutorService executor = this.listeners.remove(listener);
        if (executor != null) {
            executor.shutdownNow();
            try {
                executor.awaitTermination(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Deprecated
    @Override
    public void removeListener(Listener listener) {
        this.unregister(listener);
    }

    @Override
    public String getName() {
        return this.dispatcher.getChannel().getClusterName();
    }

    @Override
    public Membership getMembership() {
        return new ViewMembership(this.dispatcher.getChannel().getAddress(), this.view.get(), this);
    }

    @Override
    public Node getLocalMember() {
        return this.createNode(this.dispatcher.getChannel().getAddress());
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public Node createNode(Address address) {
        return this.members.computeIfAbsent(address, key -> {
            IpAddress ipAddress = (IpAddress) this.dispatcher.getChannel().down(new Event(Event.GET_PHYSICAL_ADDRESS, address));
            // Physical address might be null if node is no longer a member of the cluster
            InetSocketAddress socketAddress = (ipAddress != null) ? new InetSocketAddress(ipAddress.getIpAddress(), ipAddress.getPort()) : new InetSocketAddress(0);
            // If no logical name exists, create one using physical address
            String name = Optional.ofNullable(NameCache.get(address)).orElseGet(() -> String.format("%s:%s", socketAddress.getHostString(), socketAddress.getPort()));
            return new AddressableNode(address, name, socketAddress);
        });
    }

    @Override
    public Address getAddress(Node node) {
        return ((AddressableNode) node).getAddress();
    }

    @Override
    public void viewAccepted(View view) {
        View oldView = this.view.getAndSet(view);
        if (oldView != null) {
            List<Address> leftMembers = View.leftMembers(oldView, view);
            if (leftMembers != null) {
                this.members.keySet().removeAll(leftMembers);
            }

            if (this.listeners.isEmpty()) {
                Address localAddress = this.dispatcher.getChannel().getAddress();
                ViewMembership oldMembership = new ViewMembership(localAddress, oldView, this);
                ViewMembership membership = new ViewMembership(localAddress, view, this);
                for (Map.Entry<GroupListener, ExecutorService> entry : this.listeners.entrySet()) {
                    GroupListener listener = entry.getKey();
                    ExecutorService executor = entry.getValue();
                    try {
                        executor.submit(new ListenerTask(listener, oldMembership, membership, view instanceof MergeView));
                    } catch (RejectedExecutionException e) {
                        // Executor was shutdown
                    }
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

    private static class ListenerTask implements Runnable {
        private final GroupListener listener;
        private final Membership oldMembership;
        private final Membership membership;
        private final boolean merged;

        ListenerTask(GroupListener listener, Membership oldMembership, Membership membership, boolean merged) {
            this.listener = listener;
            this.oldMembership = oldMembership;
            this.membership = membership;
            this.merged = merged;
        }

        @Override
        public void run() {
            try {
                this.listener.membershipChanged(this.oldMembership, this.membership, this.merged);
            } catch (Throwable e) {
                ClusteringLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            }
        }
    }
}
