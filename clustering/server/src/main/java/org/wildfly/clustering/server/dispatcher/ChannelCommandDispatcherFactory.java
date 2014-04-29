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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.clustering.marshalling.MarshallingContext;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.Unmarshaller;
import org.jgroups.Address;
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
 * {@link MessageDispatcher} based {@link CommandDispatcherFactory}.
 * This factory can produce multiple {@link CommandDispatcher} instances,
 * all of which will share the same {@link MessageDispatcher} instance.
 * @author Paul Ferraro
 */
public class ChannelCommandDispatcherFactory implements CommandDispatcherFactory, RequestHandler, AutoCloseable {

    private static final short SCOPE_ID = 222;

    final Map<Object, AtomicReference<Object>> contexts = new ConcurrentHashMap<>();
    final MarshallingContext marshallingContext;

    private final Group group;
    private final MessageDispatcher dispatcher;
    private final NodeFactory<Address> nodeFactory;
    private final long timeout;

    public ChannelCommandDispatcherFactory(ChannelCommandDispatcherFactoryConfiguration config) {
        this.group = config.getGroup();
        this.nodeFactory = config.getNodeFactory();
        this.marshallingContext = config.getMarshallingContext();
        this.timeout = config.getTimeout();
        final RpcDispatcher.Marshaller marshaller = new CommandResponseMarshaller(this.marshallingContext);
        this.dispatcher = new MuxMessageDispatcher(SCOPE_ID) {
            @Override
            protected RequestCorrelator createRequestCorrelator(Protocol transport, RequestHandler handler, Address localAddr) {
                RequestCorrelator correlator = super.createRequestCorrelator(transport, handler, localAddr);
                correlator.setMarshaller(marshaller);
                return correlator;
            }
        };
        this.dispatcher.setChannel(config.getChannel());
        this.dispatcher.setRequestHandler(this);
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
                Command<Object, Object> command = (Command<Object, Object>) unmarshaller.readObject();
                AtomicReference<Object> context = this.contexts.get(clientId);
                if (context == null) return new NoSuchService();
                return command.execute(context.get());
            }
        }
    }

    @Override
    public Group getGroup() {
        return this.group;
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
        final CommandDispatcher<C> localDispatcher = new LocalCommandDispatcher<>(this.group.getLocalNode(), context);
        return new ChannelCommandDispatcher<C>(this.dispatcher, marshaller, this.nodeFactory, this.timeout, localDispatcher) {
            @Override
            public void close() {
                localDispatcher.close();
                ChannelCommandDispatcherFactory.this.contexts.remove(id);
            }
        };
    }
}
