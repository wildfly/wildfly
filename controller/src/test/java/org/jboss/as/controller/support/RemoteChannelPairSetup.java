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
package org.jboss.as.controller.support;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiverFactory;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.ServiceRegistrationException;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.remoting3.security.SimpleServerAuthenticationProvider;
import org.jboss.remoting3.spi.NetworkServerProvider;
import org.xnio.BufferAllocator;
import org.xnio.Buffers;
import org.xnio.ChannelListener;
import org.xnio.ChannelThreadPool;
import org.xnio.ChannelThreadPools;
import org.xnio.ConnectionChannelThread;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Pool;
import org.xnio.ReadChannelThread;
import org.xnio.Sequence;
import org.xnio.WriteChannelThread;
import org.xnio.Xnio;
import org.xnio.channels.AcceptingChannel;
import org.xnio.channels.ConnectedStreamChannel;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class RemoteChannelPairSetup {

    final static String ENDPOINT_NAME = "endpoint";
    final static String URI_SCHEME = "test123";
    final static String TEST_CHANNEL = "Test-Channel";
    final static int PORT = 32123;
    ChannelServer channelServer;

    protected ExecutorService executorService;
    protected ProtocolChannel serverChannel;
    protected ProtocolChannel clientChannel;

    final CountDownLatch clientConnectedLatch = new CountDownLatch(1);

    public ProtocolChannel getServerChannel() {
        return serverChannel;
    }

    public ProtocolChannel getClientChannel() {
        return clientChannel;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setupRemoting() throws IOException {
        executorService = Executors.newCachedThreadPool();
        ChannelServer.Configuration configuration = new ChannelServer.Configuration();
        configuration.setEndpointName(ENDPOINT_NAME);
        configuration.setUriScheme(URI_SCHEME);
        configuration.setBindAddress(new InetSocketAddress("::1", PORT));
        configuration.setExecutor(executorService);
        channelServer = ChannelServer.create(configuration);

        channelServer.addChannelOpenListener(TEST_CHANNEL, new OpenListener() {

            @Override
            public void registrationTerminated() {
            }

            @Override
            public void channelOpened(Channel channel) {
                serverChannel = ProtocolChannel.create(TEST_CHANNEL, channel, new ManagementChannelReceiverFactory());
                serverChannel.startReceiving();
                clientConnectedLatch.countDown();
            }
        });
    }

    public void startChannels() throws IOException, URISyntaxException {
        ProtocolChannelClient.Configuration configuration = new ProtocolChannelClient.Configuration();
        configuration.setEndpointName(ENDPOINT_NAME);
        configuration.setUriScheme(URI_SCHEME);
        configuration.setUri(new URI("" + URI_SCHEME + "://[::1]:" + PORT + ""));
        configuration.setExecutor(executorService);
        configuration.setChannelReceiverFactory(new ManagementChannelReceiverFactory());

        ProtocolChannelClient client = ProtocolChannelClient.create(configuration);
        client.connect();
        clientChannel = client.openChannel(TEST_CHANNEL);
        try {
            clientConnectedLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("ClIENT " + clientChannel);
        System.out.println("SERVER " + serverChannel);
    }

    public void stopChannels() {
        IoUtils.safeClose(clientChannel);
        IoUtils.safeClose(serverChannel);
    }

    public void shutdownRemoting() throws IOException, InterruptedException {
        channelServer.close();
        executorService.shutdown();
        executorService.awaitTermination(1L, TimeUnit.SECONDS);
        executorService.shutdownNow();
    }

    static class ChannelServer implements Closeable {
        private final Endpoint endpoint;
        private final AcceptingChannel<? extends ConnectedStreamChannel> streamServer;
        private final ReadChannelThread readChannelThread;
        private final WriteChannelThread writeChannelThread;
        private final ConnectionChannelThread connectionChannelThread;

        private ChannelServer(final Endpoint endpoint,
                AcceptingChannel<? extends ConnectedStreamChannel> streamServer,
                final ReadChannelThread readChannelThread,
                final WriteChannelThread writeChannelThread,
                final ConnectionChannelThread connectionChannelThread) {
            this.endpoint = endpoint;
            this.streamServer = streamServer;
            this.readChannelThread = readChannelThread;
            this.writeChannelThread = writeChannelThread;
            this.connectionChannelThread = connectionChannelThread;
        }

        public static ChannelServer create(final Configuration configuration) throws IOException {
            if (configuration == null) {
                throw new IllegalArgumentException("Null configuration");
            }
            configuration.validate();

            final Endpoint endpoint = Remoting.createEndpoint(configuration.getEndpointName(), configuration.getExecutor(), configuration.getOptionMap());
            final Xnio xnio = Xnio.getInstance();
            final ReadChannelThread readChannelThread = xnio.createReadChannelThread(configuration.getReadChannelThreadFactory());
            final WriteChannelThread writeChannelThread = xnio.createWriteChannelThread(configuration.getWriteChannelThreadFactory());
            final ConnectionChannelThread connectionChannelThread = xnio.createReadChannelThread(configuration.getConnectionChannelThreadFactory());

            final ChannelThreadPool<ReadChannelThread> readPool = ChannelThreadPools.singleton(readChannelThread);
            final ChannelThreadPool<WriteChannelThread> writePool = ChannelThreadPools.singleton(writeChannelThread);
            final ChannelThreadPool<ConnectionChannelThread> connectionPool = ChannelThreadPools.singleton(connectionChannelThread);
            final Pool<ByteBuffer> bufferPool = Buffers.allocatedBufferPool(BufferAllocator.BYTE_BUFFER_ALLOCATOR, 8192);
            endpoint.addConnectionProvider(configuration.getUriScheme(), new RemoteConnectionProviderFactory(xnio, bufferPool, readPool, writePool, connectionPool));

            final NetworkServerProvider networkServerProvider = endpoint.getConnectionProviderInterface(configuration.getUriScheme(), NetworkServerProvider.class);
            SimpleServerAuthenticationProvider provider = new SimpleServerAuthenticationProvider();
            //There is currently a probable bug in jboss remoting, so the user realm name MUST be the same as
            //the endpoint name.
            provider.addUser("bob", configuration.getEndpointName(), "pass".toCharArray());
            ChannelListener<AcceptingChannel<ConnectedStreamChannel>> serverListener = networkServerProvider.getServerListener(OptionMap.create(Options.SASL_MECHANISMS, Sequence.of("DIGEST-MD5")), provider);

            AcceptingChannel<? extends ConnectedStreamChannel> streamServer = xnio.createStreamServer(configuration.getBindAddress(), connectionChannelThread, serverListener, OptionMap.EMPTY);

            return new ChannelServer(endpoint, streamServer, readChannelThread, writeChannelThread, connectionChannelThread);
        }

        public void addChannelOpenListener(final String channelName) throws ServiceRegistrationException {
            addChannelOpenListener(channelName, null);
        }

        public void addChannelOpenListener(final String channelName, final OpenListener openListener) throws ServiceRegistrationException {
            endpoint.registerService(channelName, new OpenListener() {
                public void channelOpened(final Channel channel) {
                    if (openListener != null) {
                        openListener.channelOpened(channel);
                    }
                }

                public void registrationTerminated() {
                    if (openListener != null) {
                        openListener.registrationTerminated();
                    }
                }
            }, OptionMap.EMPTY);

        }

        public void close() {
            IoUtils.safeClose(streamServer);
            IoUtils.safeClose(endpoint);

            //TODO do I need to shut down the executor or will this be injected in from somewhere and so should be kept alive?
//            executorService.shutdown();
//            executorService.awaitTermination(1L, TimeUnit.DAYS);
//            executorService.shutdownNow();
            readChannelThread.shutdown();
            writeChannelThread.shutdown();
            connectionChannelThread.shutdown();

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
                    throw new IllegalArgumentException("Null bind address");
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

}
