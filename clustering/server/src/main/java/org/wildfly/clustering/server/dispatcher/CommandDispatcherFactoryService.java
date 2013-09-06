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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.clustering.marshalling.DynamicClassTable;
import org.jboss.as.clustering.marshalling.MarshallingConfigurationFactory;
import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.jboss.as.clustering.marshalling.SimpleMarshallingContextFactory;
import org.jboss.as.clustering.marshalling.VersionedMarshallingConfiguration;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestCorrelator;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.blocks.mux.MuxMessageDispatcher;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.NodeFactory;

/**
 * Service providing a CommandDispatcherFactory.
 * Multiple command dispatchers share a single {@link MessageDispatcher}.
 * @author Paul Ferraro
 */
public class CommandDispatcherFactoryService implements CommandDispatcherFactory, RequestHandler, Service<CommandDispatcherFactory>, VersionedMarshallingConfiguration {

    private static final short SCOPE_ID = 222;
    private static final int CURRENT_VERSION = 1;

    final Map<Object, AtomicReference<Object>> contexts = new ConcurrentHashMap<>();

    private final Map<Integer, MarshallingConfiguration> configurations = new HashMap<>();
    private final CommandDispatcherFactoryConfiguration config;

    volatile MarshallingContext marshallingContext;
    volatile NodeFactory<Address> factory = null;

    private volatile Group group = null;
    private volatile MessageDispatcher dispatcher = null;
    private volatile long timeout = TimeUnit.MINUTES.toMillis(1);

    public CommandDispatcherFactoryService(CommandDispatcherFactoryConfiguration config) {
        this.config = config;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.factory = this.config.getNodeFactory();
        this.group = this.config.getGroup();

        ModuleLoader loader = this.config.getModuleLoader();
        MarshallingConfiguration config = MarshallingConfigurationFactory.createMarshallingConfiguration(loader);
        try {
            Module module = loader.loadModule(this.config.getModuleIdentifier());
            config.setClassTable(new DynamicClassTable(module.getClassLoader()));
            this.configurations.put(CURRENT_VERSION, config);
            this.marshallingContext = new SimpleMarshallingContextFactory().createMarshallingContext(this, module.getClassLoader());
        } catch (ModuleLoadException e) {
            throw new StartException(e);
        }

        Channel channel = this.config.getChannel();
        final RpcDispatcher.Marshaller marshaller = new CommandResponseMarshaller(this.marshallingContext, CURRENT_VERSION);
        this.dispatcher = new MuxMessageDispatcher(SCOPE_ID) {
            @Override
            protected RequestCorrelator createRequestCorrelator(Protocol transport, RequestHandler handler, Address localAddr) {
                RequestCorrelator correlator = super.createRequestCorrelator(transport, handler, localAddr);
                correlator.setMarshaller(marshaller);
                return correlator;
            }
        };
        this.dispatcher.setChannel(channel);
        this.dispatcher.setRequestHandler(this);
        this.dispatcher.start();
    }

    @Override
    public void stop(StopContext context) {
        try {
            this.dispatcher.stop();
        } finally {
            this.configurations.clear();
        }
    }

    @Override
    public CommandDispatcherFactory getValue() {
        return this;
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(final Object id, C context) {
        final int version = CURRENT_VERSION;
        CommandMarshaller<C> marshaller = new CommandMarshaller<C>() {
            @Override
            public <R> byte[] marshal(Command<R, C> command) throws IOException {
                try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    output.write(version);
                    try (Marshaller marshaller = CommandDispatcherFactoryService.this.marshallingContext.createMarshaller(version)) {
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
        return new ServiceCommandDispatcher<C>(this.dispatcher, marshaller, this.factory, this.timeout) {
            @Override
            public void close() {
                CommandDispatcherFactoryService.this.contexts.remove(id);
            }
        };
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
    public Object handle(Message message) throws Exception {
        try (InputStream input = new ByteArrayInputStream(message.getRawBuffer(), message.getOffset(), message.getLength())) {
            int version = input.read();
            try (Unmarshaller unmarshaller = this.marshallingContext.createUnmarshaller(version)) {
                unmarshaller.start(Marshalling.createByteInput(input));
                Object clientId = unmarshaller.readObject();
                Command<Object, Object> command = (Command<Object, Object>) unmarshaller.readObject();
                AtomicReference<Object> context = this.contexts.get(clientId);
                if (context == null) return new NoSuchService();
                return command.execute(context.get());
            }
        }
    }
}
