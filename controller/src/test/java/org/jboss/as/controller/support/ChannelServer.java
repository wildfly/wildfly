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
import java.util.concurrent.Executor;

import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.ServiceRegistrationException;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.remoting3.security.SimpleServerAuthenticationProvider;
import org.jboss.remoting3.spi.NetworkServerProvider;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.Xnio;
import org.xnio.channels.AcceptingChannel;
import org.xnio.channels.ConnectedStreamChannel;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ChannelServer implements Closeable {
    private final Endpoint endpoint;
    private final AcceptingChannel<? extends ConnectedStreamChannel> streamServer;

    private ChannelServer(final Endpoint endpoint,
            AcceptingChannel<? extends ConnectedStreamChannel> streamServer) {
        this.endpoint = endpoint;
        this.streamServer = streamServer;
    }

    public static ChannelServer create(final Configuration configuration) throws IOException {
        if (configuration == null) {
            throw new IllegalArgumentException("Null configuration");
        }
        configuration.validate();

        final Endpoint endpoint = Remoting.createEndpoint(configuration.getEndpointName(), configuration.getExecutor(), configuration.getOptionMap());
        final Xnio xnio = Xnio.getInstance();

        endpoint.addConnectionProvider(configuration.getUriScheme(), new RemoteConnectionProviderFactory(xnio), OptionMap.create(Options.SSL_ENABLED, false));

        final NetworkServerProvider networkServerProvider = endpoint.getConnectionProviderInterface(configuration.getUriScheme(), NetworkServerProvider.class);
        SimpleServerAuthenticationProvider provider = new SimpleServerAuthenticationProvider();
        //There is currently a probable bug in jboss remoting, so the user realm name MUST be the same as
        //the endpoint name.
        provider.addUser("bob", configuration.getEndpointName(), "pass".toCharArray());
        //ChannelListener<AcceptingChannel<ConnectedStreamChannel>> serverListener = networkServerProvider.getServerListener(OptionMap.create(Options.SASL_MECHANISMS, Sequence.of("DIGEST-MD5")), provider);
        System.out.println(configuration.getBindAddress());
        //AcceptingChannel<? extends ConnectedStreamChannel> streamServer = xnio.createStreamServer(configuration.getBindAddress(), connectionChannelThread, serverListener, OptionMap.EMPTY);
        AcceptingChannel<? extends ConnectedStreamChannel> streamServer = networkServerProvider.createServer(configuration.getBindAddress(), OptionMap.create(Options.SASL_MECHANISMS, Sequence.of("CRAM-MD5")), provider);

        return new ChannelServer(endpoint, streamServer);
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
//        executorService.shutdown();
//        executorService.awaitTermination(1L, TimeUnit.DAYS);
//        executorService.shutdownNow();
    }

    public static final class Configuration {
        private String endpointName;
        private OptionMap optionMap = OptionMap.EMPTY;
        private ThreadGroup readChannelThreadFactory;
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
