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
package org.wildfly.clustering.server.dispatcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.concurrent.ComparableRunnableFuture;
import org.jboss.as.clustering.marshalling.DynamicClassTable;
import org.jboss.as.clustering.marshalling.MarshallingConfigurationFactory;
import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.jboss.as.clustering.marshalling.VersionedMarshallingConfiguration;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.jboss.threads.JBossThreadFactory;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Event;
import org.jgroups.MergeView;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.blocks.mux.MuxMessageDispatcher;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.Node;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.dispatcher.MembershipListener;
import org.wildfly.clustering.server.SimpleNode;
import org.wildfly.security.manager.GetAccessControlContextAction;

/**
 * Service providing a CommandDispatcherFactory.
 * Multiple command dispatchers share a single MessageDispatcher.
 * @author Paul Ferraro
 */
public class CommandDispatcherFactoryService implements CommandDispatcherFactory, RequestHandler, Service<CommandDispatcherFactory>, org.jgroups.MembershipListener, NodeRegistry, VersionedMarshallingConfiguration {

    private static final short SCOPE_ID = 222;
    private static final int CURRENT_VERSION = 1;

    private final MarshallingContext marshallingContext = new MarshallingContext(this);
    final Map<ServiceName, Map.Entry<MembershipListener, Object>> services = new ConcurrentHashMap<>();
    final ConcurrentMap<Address, Node> nodes = new ConcurrentHashMap<>();
    private final Map<Integer, MarshallingConfiguration> configurations = new HashMap<>();
    private final Value<Channel> channel;
    private final Value<ModuleLoader> loader;
    private final ModuleIdentifier moduleId;
    private volatile ExecutorService executor;
    private volatile MessageDispatcher dispatcher;
    volatile Set<Node> view = Collections.emptySet();
    private volatile long timeout = TimeUnit.MINUTES.toMillis(1);

    public CommandDispatcherFactoryService(Value<Channel> channel, Value<ModuleLoader> loader, ModuleIdentifier moduleId) {
        this.channel = channel;
        this.loader = loader;
        this.moduleId = moduleId;
    }

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(ServiceName service, C context) {
        return this.createCommandDispatcher(service, context, null);
    }

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(ServiceName service, C context, MembershipListener listener) {
        return new ServiceCommandDispatcher<>(this.dispatcher, new ServiceCommandMarshaller<>(service, context, listener, this.services, this.marshallingContext, CURRENT_VERSION), this, this.timeout);
    }

    @Override
    public CommandDispatcherFactory getValue() {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ModuleLoader loader = this.loader.getValue();
        MarshallingConfiguration config = MarshallingConfigurationFactory.createMarshallingConfiguration(loader);
        try {
            Module module = loader.loadModule(this.moduleId);
            config.setClassTable(new DynamicClassTable(module.getClassLoader()));
            this.configurations.put(CURRENT_VERSION, config);
        } catch (ModuleLoadException e) {
            throw new StartException(e);
        }

        ThreadGroup group = new ThreadGroup(CommandDispatcherFactoryService.class.getSimpleName());
        ThreadFactory factory = new JBossThreadFactory(group, Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>(2), factory) {
            @Override
            protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
                RunnableFuture<T> future = super.newTaskFor(runnable, value);
                return (runnable instanceof ViewTask) ? new ComparableRunnableFuture<>(future, (ViewTask) runnable) : future;
            }
        };

        final RpcDispatcher.Marshaller marshaller = new CommandResponseMarshaller(this.marshallingContext, CURRENT_VERSION);
        this.dispatcher = new MuxMessageDispatcher(SCOPE_ID) {
            @Override
            protected RequestCorrelator createRequestCorrelator(Protocol transport, RequestHandler handler, Address localAddr) {
                RequestCorrelator correlator = super.createRequestCorrelator(transport, handler, localAddr);
                correlator.setMarshaller(marshaller);
                return correlator;
            }
        };
        this.dispatcher.setChannel(this.channel.getValue());
        this.dispatcher.setRequestHandler(this);
        this.dispatcher.setMembershipListener(this);
        this.dispatcher.start();
        Set<Node> view = new HashSet<>();
        for (Address address: this.dispatcher.getChannel().getView().getMembers()) {
            view.add(this.getNode(address));
        }
        this.view = view;
    }

    @Override
    public void stop(StopContext context) {
        this.dispatcher.stop();
        this.executor.shutdown();
        this.configurations.clear();
    }

    @Override
    public int getCurrentMarshallingVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration(int version) {
        MarshallingConfiguration configuration = this.configurations.get(version);
        if (configuration == null) {
            throw new IllegalArgumentException(Integer.toString(version));
        }
        return configuration;
    }

    @Override
    public Node getNode(Address address) {
        Node node = this.nodes.get(address);
        if (node != null) return node;
        Channel channel = this.dispatcher.getChannel();
        IpAddress ipAddress = (IpAddress) channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, address));
        InetSocketAddress socketAddress = new InetSocketAddress(ipAddress.getIpAddress(), ipAddress.getPort());
        String name = channel.getName(address);
        if (name == null) {
            name = String.format("%s:%s", socketAddress.getHostString(), socketAddress.getPort());
        }
        node = new SimpleNode(name, socketAddress);
        Node existing = this.nodes.putIfAbsent(address, node);
        return (existing != null) ? existing : node;
    }

    @Override
    public Address getAddress(Node node) {
        for (Map.Entry<Address, Node> entry: this.nodes.entrySet()) {
            if (node.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException(node.getName());
    }

    private Address getLocalAddress() {
        return this.dispatcher.getChannel().getAddress();
    }

    @Override
    public List<Node> getNodes() {
        List<Address> addresses = this.dispatcher.getChannel().getView().getMembers();
        List<Node> nodes = new ArrayList<>(addresses.size());
        for (Address address: addresses) {
            nodes.add(this.getNode(address));
        }
        return nodes;
    }

    @Override
    public boolean isCoordinator() {
        return this.getLocalAddress().equals(this.getCoordinatorAddress());
    }

    @Override
    public Node getLocalNode() {
        return this.getNode(this.getLocalAddress());
    }

    @Override
    public Node getCoordinatorNode() {
        Address address = this.getCoordinatorAddress();
        return (address != null) ? this.getNode(address) : null;
    }

    private Address getCoordinatorAddress() {
        List<Address> addresses = this.dispatcher.getChannel().getView().getMembers();
        return !addresses.isEmpty() ? addresses.get(0) : null;
    }

    @Override
    public Object handle(Message message) throws Exception {
        try (InputStream input = new ByteArrayInputStream(message.getRawBuffer(), message.getOffset(), message.getLength())) {
            int version = input.read();
            try (Unmarshaller unmarshaller = this.marshallingContext.createUnmarshaller(version)) {
                unmarshaller.start(Marshalling.createByteInput(input));
                ServiceName service = ServiceName.parse(unmarshaller.readUTF());
                Command<Object, Object> command = (Command<Object, Object>) unmarshaller.readObject();
                Map.Entry<MembershipListener, Object> entry = this.services.get(service);
                if (entry == null) return new NoSuchService();
                Object context = entry.getValue();
                return command.execute(context);
            }
        }
    }

    @Override
    public void viewAccepted(View view) {
        this.executor.submit(new ViewTask(view));
    }

    @Override
    public void suspect(Address suspected) {
        // Do nothing
    }

    @Override
    public void block() {
        // Do nothing
    }

    @Override
    public void unblock() {
        // Do nothing
    }

    private class ViewTask implements Runnable, Comparable<ViewTask> {
        private final View view;

        ViewTask(View view) {
            this.view = view;
        }

        @Override
        public void run() {
            Set<Node> previousView = CommandDispatcherFactoryService.this.view;
            int size = this.view.size();
            final List<Node> allNodes = new ArrayList<>(size);
            final List<Node> newNodes = new ArrayList<>(size);
            for (Address address: this.view.getMembers()) {
                Node node = CommandDispatcherFactoryService.this.getNode(address);
                allNodes.add(node);
                if (!previousView.contains(node)) {
                    newNodes.add(node);
                }
            }
            final List<Node> deadNodes = new ArrayList<>(previousView);
            deadNodes.removeAll(allNodes);
            CommandDispatcherFactoryService.this.view = new HashSet<>(allNodes);
            CommandDispatcherFactoryService.this.nodes.values().removeAll(deadNodes);

            final Collection<Map.Entry<MembershipListener, Object>> listeners = CommandDispatcherFactoryService.this.services.values();
            final List<List<Node>> groups = this.createGroups(this.view);
            for (Map.Entry<MembershipListener, Object> entry: listeners) {
                MembershipListener listener = entry.getKey();
                if (listener != null) {
                    listener.membershipChanged(deadNodes, newNodes, allNodes, groups);
                }
            }
        }

        private List<List<Node>> createGroups(View view) {
            if (!(view instanceof MergeView)) return null;
            MergeView merge = (MergeView) view;
            List<View> partitions = merge.getSubgroups();
            final List<List<Node>> groups = new ArrayList<>(partitions.size());
            for (View partition: partitions) {
                List<Node> nodes = new ArrayList<>(partition.size());
                for (Address address: partition.getMembers()) {
                    nodes.add(CommandDispatcherFactoryService.this.getNode(address));
                }
                groups.add(nodes);
            }
            return groups;
        }

        @Override
        public int compareTo(ViewTask task) {
            return this.view.getViewId().compareTo(task.view.getViewId());
        }
    }
}
