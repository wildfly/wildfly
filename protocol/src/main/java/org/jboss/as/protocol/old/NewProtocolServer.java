/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.protocol.old;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.remoting3.spi.NetworkServerProvider;
import org.xnio.BufferAllocator;
import org.xnio.Buffers;
import org.xnio.ChannelListener;
import org.xnio.ChannelThreadPool;
import org.xnio.ChannelThreadPools;
import org.xnio.ConnectionChannelThread;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Pool;
import org.xnio.ReadChannelThread;
import org.xnio.WriteChannelThread;
import org.xnio.Xnio;
import org.xnio.channels.AcceptingChannel;
import org.xnio.channels.ConnectedStreamChannel;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class NewProtocolServer implements Closeable {
    private final Endpoint endpoint;
    private final AcceptingChannel<? extends ConnectedStreamChannel> streamServer;
    private final ReadChannelThread readChannelThread;
    private final WriteChannelThread writeChannelThread;
    private final ConnectionChannelThread connectionChannelThread;
    private final AtomicReference<Channel> passer;

    private NewProtocolServer(final Endpoint endpoint,
            AcceptingChannel<? extends ConnectedStreamChannel> streamServer,
            final ReadChannelThread readChannelThread,
            final WriteChannelThread writeChannelThread,
            final ConnectionChannelThread connectionChannelThread,
            final AtomicReference<Channel> passer) {
        this.endpoint = endpoint;
        this.streamServer = streamServer;
        this.readChannelThread = readChannelThread;
        this.writeChannelThread = writeChannelThread;
        this.connectionChannelThread = connectionChannelThread;
        this.passer = passer;
    }

    public static NewProtocolServer createProtocolServer(final Configuration configuration) throws IOException {
        if (configuration == null) {
            throw new IllegalArgumentException("Null configuration");
        }
        configuration.validate();

        final Endpoint endpoint = Remoting.createEndpoint(configuration.getEndpointName(), configuration.getExecutor(), configuration.getOptionMap());
        final Xnio xnio = Xnio.getInstance();
        final ReadChannelThread readChannelThread = xnio.createReadChannelThread(Executors.defaultThreadFactory());
        final WriteChannelThread writeChannelThread = xnio.createWriteChannelThread(Executors.defaultThreadFactory());
        final ConnectionChannelThread connectionChannelThread = xnio.createReadChannelThread(Executors.defaultThreadFactory());

        final ChannelThreadPool<ReadChannelThread> readPool = ChannelThreadPools.singleton(readChannelThread);
        final ChannelThreadPool<WriteChannelThread> writePool = ChannelThreadPools.singleton(writeChannelThread);
        final ChannelThreadPool<ConnectionChannelThread> connectionPool = ChannelThreadPools.singleton(connectionChannelThread);
        final Pool<ByteBuffer> bufferPool = Buffers.allocatedBufferPool(BufferAllocator.BYTE_BUFFER_ALLOCATOR, 8192);
        endpoint.addConnectionProvider(configuration.getUriScheme(), new RemoteConnectionProviderFactory(xnio, bufferPool, readPool, writePool, connectionPool));

        final NetworkServerProvider networkServerProvider = endpoint.getConnectionProviderInterface("remote", NetworkServerProvider.class);
        final ServerAuthenticationProvider provider = new ServerAuthenticationProvider() {

            @Override
            public CallbackHandler getCallbackHandler(String mechanismName) {
                return new CallbackHandler() {

                    @Override
                    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                        //Don't worry about security for now
                    }
                };
            }
        };

        final ChannelListener<AcceptingChannel<ConnectedStreamChannel>> serverListener = networkServerProvider.getServerListener(OptionMap.EMPTY, provider);
        AcceptingChannel<? extends ConnectedStreamChannel> streamServer = xnio.createStreamServer(configuration.getBindAddress(), connectionChannelThread, serverListener, OptionMap.EMPTY);
        final AtomicReference<Channel> passer = new AtomicReference<Channel>();
        endpoint.registerService("org.jboss.test", new OpenListener() {
            public void channelOpened(final Channel channel) {
                passer.set(channel);
            }

            public void registrationTerminated() {
            }
        }, OptionMap.EMPTY);

        return new NewProtocolServer(endpoint, streamServer, readChannelThread, writeChannelThread, connectionChannelThread, passer);
    }

    public void close() {
        IoUtils.safeClose(streamServer);
        IoUtils.safeClose(endpoint);

        //TODO do I need to shut down the executor or will this be injected in from somewhere and so should be kept alive?
//        executorService.shutdown();
//        executorService.awaitTermination(1L, TimeUnit.DAYS);
//        executorService.shutdownNow();
        readChannelThread.shutdown();
        writeChannelThread.shutdown();
        connectionChannelThread.shutdown();
        passer.set(null);

    }

    public static final class Configuration {
        private String endpointName;
        private OptionMap optionMap = OptionMap.EMPTY;
        private ThreadFactory readChannelThreadFactory = Executors.defaultThreadFactory();
        private ThreadFactory writeChannelThreadFactory = Executors.defaultThreadFactory();
        private ThreadFactory connectionChannelThreadFactory = Executors.defaultThreadFactory();
        private String uriScheme;
        private InetSocketAddress bindAddress;
        private Executor executor;

        public Configuration() {
        }

        void validate() {
            if (endpointName == null) {
                throw new IllegalArgumentException("Null endpoint name");
            }
            if (optionMap == null) {
                throw new IllegalArgumentException("Null option map");
            }
            if (readChannelThreadFactory == null) {
                throw new IllegalArgumentException("Null read channel thread factory");
            }
            if (writeChannelThreadFactory == null) {
                throw new IllegalArgumentException("Null write channel thread factory");
            }
            if (connectionChannelThreadFactory == null) {
                throw new IllegalArgumentException("Null connection channel thread factory");
            }
            if (uriScheme == null) {
                throw new IllegalArgumentException("Null protocol name");
            }
            if (bindAddress == null) {
                throw new IllegalArgumentException("Null port");
            }
        }

        public void setEndpointName(String endpointName) {
            this.endpointName = endpointName;
        }

        public String getEndpointName() {
            return endpointName;
        }

        public ThreadFactory getReadChannelThreadFactory() {
            return readChannelThreadFactory;
        }

        public void setReadChannelThreadFactory(ThreadFactory readChannelThreadFactory) {
            this.readChannelThreadFactory = readChannelThreadFactory;
        }

        public ThreadFactory getWriteChannelThreadFactory() {
            return writeChannelThreadFactory;
        }

        public void setWriteChannelThreadFactory(ThreadFactory writeChannelThreadFactory) {
            this.writeChannelThreadFactory = writeChannelThreadFactory;
        }

        public ThreadFactory getConnectionChannelThreadFactory() {
            return connectionChannelThreadFactory;
        }

        public void setConnectionChannelThreadFactory(ThreadFactory connectionChannelThreadFactory) {
            this.connectionChannelThreadFactory = connectionChannelThreadFactory;
        }

        public String getUriScheme() {
            return uriScheme;
        }

        public void setUriScheme(String uriScheme) {
            this.uriScheme = uriScheme;
        }

        public OptionMap getOptionMap() {
            return optionMap;
        }

        public void setOptionMap(OptionMap optionMap) {
            this.optionMap = optionMap;
        }

        public InetSocketAddress getBindAddress() {
            return bindAddress;
        }

        public void setBindAddress(final InetSocketAddress bindAddress) {
            this.bindAddress = bindAddress;
        }

        public Executor getExecutor() {
            return executor;
        }

        public void setExecutor(final Executor readExecutor) {
            this.executor = readExecutor;
        }
    }

}
