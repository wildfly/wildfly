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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jboss.as.clustering.context.Contextualizer;
import org.jboss.as.clustering.context.DefaultContextualizer;
import org.jboss.as.clustering.context.DefaultExecutorService;
import org.jboss.as.clustering.context.DefaultThreadFactory;
import org.jboss.as.clustering.context.ExecutorServiceFactory;
import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.ModuleLoader;
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
import org.wildfly.clustering.marshalling.jboss.DynamicClassTable;
import org.wildfly.clustering.marshalling.jboss.ExternalizerObjectTable;
import org.wildfly.clustering.marshalling.jboss.JBossByteBufferMarshaller;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamByteBufferMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContextBuilder;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshalledValueFactory;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.server.group.AddressableNode;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.service.concurrent.ServiceExecutor;
import org.wildfly.clustering.service.concurrent.StampedLockServiceExecutor;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.common.function.Functions;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link MessageDispatcher} based {@link CommandDispatcherFactory}.
 * This factory can produce multiple {@link CommandDispatcher} instances,
 * all of which will share the same {@link MessageDispatcher} instance.
 * @author Paul Ferraro
 */
public class ChannelCommandDispatcherFactory implements AutoCloseableCommandDispatcherFactory, RequestHandler, org.wildfly.clustering.spi.group.Group<Address>, MembershipListener, Runnable, Function<GroupListener, ExecutorService> {

    enum MarshallingVersion implements Function<MarshallingConfigurationContext, MarshallingConfiguration> {
        VERSION_1() {
            @Override
            public MarshallingConfiguration apply(MarshallingConfigurationContext context) {
                MarshallingConfiguration config = new MarshallingConfiguration();
                ClassLoader userLoader = context.getClassLoader();
                ClassLoader loader = WildFlySecurityManager.getClassLoaderPrivileged(ChannelCommandDispatcherFactory.class);
                ClassLoader[] loaders = userLoader.equals(loader) ? new ClassLoader[] { userLoader } : new ClassLoader[] { userLoader, loader };
                config.setClassResolver(ModularClassResolver.getInstance(context.getModuleLoader()));
                config.setClassTable(new DynamicClassTable(loaders));
                config.setObjectTable(new ExternalizerObjectTable(loaders));
                return config;
            }
        },
        ;
        static final MarshallingVersion CURRENT = VERSION_1;
    }

    static final Optional<Object> NO_SUCH_SERVICE = Optional.of(NoSuchService.INSTANCE);
    static final ExceptionSupplier<Object, Exception> NO_SUCH_SERVICE_SUPPLIER = Functions.constantExceptionSupplier(NoSuchService.INSTANCE);

    private final ConcurrentMap<Address, Node> members = new ConcurrentHashMap<>();
    private final Map<Object, CommandDispatcherContext<?, ?>> contexts = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool(new DefaultThreadFactory(this.getClass()));
    private final ServiceExecutor executor = new StampedLockServiceExecutor();
    private final Map<GroupListener, ExecutorService> listeners = new ConcurrentHashMap<>();
    private final AtomicReference<View> view = new AtomicReference<>();
    private final ByteBufferMarshaller marshaller;
    private final MessageDispatcher dispatcher;
    private final Duration timeout;
    private final ModuleLoader loader;

    @SuppressWarnings("resource")
    public ChannelCommandDispatcherFactory(ChannelCommandDispatcherFactoryConfiguration config) {
        this.marshaller = config.getMarshaller();
        this.timeout = config.getTimeout();
        this.loader = config.getModuleLoader();
        JChannel channel = config.getChannel();
        RequestCorrelator correlator = new RequestCorrelator(channel.getProtocolStack(), this, channel.getAddress()).setMarshaller(new CommandResponseMarshaller(config));
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
        this.shutdown(this.executorService);
        this.dispatcher.stop();
        this.dispatcher.getChannel().setUpHandler(null);
        // Cleanup any stray listeners
        for (ExecutorService executor : this.listeners.values()) {
            this.shutdown(executor);
        }
        this.listeners.clear();
    }

    @Override
    public void close() {
        this.executor.close(this);
    }

    private void shutdown(ExecutorService executor) {
        WildFlySecurityManager.doUnchecked(executor, DefaultExecutorService.SHUTDOWN_NOW_ACTION);
        try {
            executor.awaitTermination(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Object handle(Message request) throws Exception {
        return this.read(request).get();
    }

    @Override
    public void handle(Message request, Response response) throws Exception {
        ExceptionSupplier<Object, Exception> commandTask = this.read(request);
        Runnable responseTask = new Runnable() {
            @Override
            public void run() {
                try {
                    response.send(commandTask.get(), false);
                } catch (Throwable e) {
                    response.send(e, true);
                }
            }
        };
        try {
            this.executorService.submit(responseTask);
        } catch (RejectedExecutionException e) {
            response.send(NoSuchService.INSTANCE, false);
        }
    }

    private ExceptionSupplier<Object, Exception> read(Message message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getRawBuffer(), message.getOffset(), message.getLength());
        @SuppressWarnings("unchecked")
        Map.Entry<Object, MarshalledValue<Command<Object, Object>, Object>> entry = (Map.Entry<Object, MarshalledValue<Command<Object, Object>, Object>>) this.marshaller.read(buffer);
        Object clientId = entry.getKey();
        CommandDispatcherContext<?, ?> context = this.contexts.get(clientId);
        if (context == null) return NO_SUCH_SERVICE_SUPPLIER;
        Object commandContext = context.getCommandContext();
        Contextualizer contextualizer = context.getContextualizer();
        MarshalledValue<Command<Object, Object>, Object> value = entry.getValue();
        Command<Object, Object> command = value.get(context.getMarshalledValueFactory().getMarshallingContext());
        ExceptionSupplier<Object, Exception> commandExecutionTask = new ExceptionSupplier<Object, Exception>() {
            @Override
            public Object get() throws Exception {
                return context.getMarshalledValueFactory().createMarshalledValue(command.execute(commandContext));
            }
        };
        ServiceExecutor executor = this.executor;
        return new ExceptionSupplier<Object, Exception>() {
            @Override
            public Object get() throws Exception {
                return executor.execute(contextualizer.contextualize(commandExecutionTask)).orElse(NO_SUCH_SERVICE);
            }
        };
    }

    @Override
    public Group getGroup() {
        return this;
    }

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(Object id, C commandContext, ClassLoader loader) {
        ByteBufferMarshaller dispatcherMarshaller = this.createMarshaller(loader);
        MarshalledValueFactory<ByteBufferMarshaller> factory = new ByteBufferMarshalledValueFactory(dispatcherMarshaller);
        Contextualizer contextualizer = new DefaultContextualizer();
        CommandDispatcherContext<C, ByteBufferMarshaller> context = new CommandDispatcherContext<C, ByteBufferMarshaller>() {
            @Override
            public C getCommandContext() {
                return commandContext;
            }

            @Override
            public Contextualizer getContextualizer() {
                return contextualizer;
            }

            @Override
            public MarshalledValueFactory<ByteBufferMarshaller> getMarshalledValueFactory() {
                return factory;
            }
        };
        if (this.contexts.putIfAbsent(id, context) != null) {
            throw ClusteringServerLogger.ROOT_LOGGER.commandDispatcherAlreadyExists(id);
        }
        CommandMarshaller<C> marshaller = new CommandDispatcherMarshaller<>(this.marshaller, id, factory);
        CommandDispatcher<C> localDispatcher = new LocalCommandDispatcher<>(this.getLocalMember(), commandContext);
        return new ChannelCommandDispatcher<>(this.dispatcher, marshaller, dispatcherMarshaller, this, this.timeout, localDispatcher, () -> {
            localDispatcher.close();
            this.contexts.remove(id);
        });
    }

    private ByteBufferMarshaller createMarshaller(ClassLoader loader) {
        try {
            return new ProtoStreamByteBufferMarshaller(new SerializationContextBuilder().register(loader).build());
        } catch (NoSuchElementException e) {
            ModuleLoader moduleLoader = this.loader;
            MarshallingConfigurationContext marshallingConfigurationContext = new MarshallingConfigurationContext() {
                @Override
                public ClassLoader getClassLoader() {
                    return loader;
                }

                @Override
                public ModuleLoader getModuleLoader() {
                    return moduleLoader;
                }
            };
            return new JBossByteBufferMarshaller(new SimpleMarshallingConfigurationRepository(MarshallingVersion.class, MarshallingVersion.CURRENT, marshallingConfigurationContext), loader);
        }
    }

    @Override
    public Registration register(GroupListener listener) {
        this.listeners.computeIfAbsent(listener, this);
        return () -> this.unregister(listener);
    }

    @Override
    public ExecutorService apply(GroupListener listener) {
        return new DefaultExecutorService(listener.getClass(), ExecutorServiceFactory.SINGLE_THREAD);
    }

    private void unregister(GroupListener listener) {
        ExecutorService executor = this.listeners.remove(listener);
        if (executor != null) {
            this.shutdown(executor);
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

            if (!this.listeners.isEmpty()) {
                Address localAddress = this.dispatcher.getChannel().getAddress();
                ViewMembership oldMembership = new ViewMembership(localAddress, oldView, this);
                ViewMembership membership = new ViewMembership(localAddress, view, this);
                for (Map.Entry<GroupListener, ExecutorService> entry : this.listeners.entrySet()) {
                    GroupListener listener = entry.getKey();
                    ExecutorService executor = entry.getValue();
                    Runnable listenerTask = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                listener.membershipChanged(oldMembership, membership, view instanceof MergeView);
                            } catch (Throwable e) {
                                ClusteringLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                            }
                        }
                    };
                    try {
                        executor.submit(listenerTask);
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
}
