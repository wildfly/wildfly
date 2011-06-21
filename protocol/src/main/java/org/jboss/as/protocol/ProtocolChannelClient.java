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
package org.jboss.as.protocol;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.modules.ModuleLoadException;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.IoFuture;
import org.xnio.IoFuture.Status;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

/**
 * This class is not thread safe and should only be used by one thread
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ProtocolChannelClient<T extends ProtocolChannel> implements Closeable {
    private final boolean startedEndpoint;
    private final Endpoint endpoint;
    private final Registration providerRegistration;
    private final URI uri;
    private final ProtocolChannelFactory<T> channelFactory;
    private volatile Connection connection;
    private final Set<T> channels = new HashSet<T>();
    private boolean closed;

//    private ProtocolChannelClient(final Endpoint endpoint,
//            final URI uri,
//            final ProtocolChannelFactory<T> channelFactory) {
//        this(false, endpoint, uri, channelFactory);
//    }

//    private ProtocolChannelClient(final Endpoint endpoint,
//            final URI uri,
//            final ReadChannelThread readChannelThread,
//            final WriteChannelThread writeChannelThread,
//            final ConnectionChannelThread connectionChannelThread,
//            final ProtocolChannelFactory<T> channelFactory) {
//        this(true, endpoint, uri, readChannelThread, writeChannelThread, connectionChannelThread, channelFactory);
//    }

    private ProtocolChannelClient(final boolean startedEndpoint,
            final Endpoint endpoint,
            final Registration providerRegistration,
            final URI uri,
            final ProtocolChannelFactory<T> channelFactory) {
        this.startedEndpoint = startedEndpoint;
        this.endpoint = endpoint;
        this.providerRegistration = providerRegistration;
        this.uri = uri;
        this.channelFactory = channelFactory;
    }

    public static <T extends ProtocolChannel> ProtocolChannelClient<T> create(final Configuration<T> configuration) throws IOException, URISyntaxException {
        if (configuration == null) {
            throw new IllegalArgumentException("Null configuration");
        }
        configuration.validate();

        final Endpoint endpoint;
        if (configuration.getEndpoint() != null) {
            endpoint = configuration.getEndpoint();
            return new ProtocolChannelClient<T>(false, endpoint, null, configuration.getUri(), configuration.getChannelFactory());
        } else {
            endpoint = Remoting.createEndpoint(configuration.getEndpointName(), configuration.getExecutor(), configuration.getOptionMap());
            Xnio xnio;
            try {
                xnio = XnioUtil.getXnio();
            } catch (ModuleLoadException e) {
                throw new RuntimeException(e);
            }

            Registration providerRegistration = endpoint.addConnectionProvider(configuration.getUri().getScheme(), new RemoteConnectionProviderFactory(xnio), OptionMap.create(Options.SSL_ENABLED, false));
            return new ProtocolChannelClient<T>(true, endpoint, providerRegistration, configuration.getUri(), configuration.getChannelFactory());
        }
    }

    public Connection connect() throws IOException {
        if (closed) {
            throw new IllegalStateException("Closed this client");
        }
        if (connection != null) {
            throw new IllegalStateException("Already connected");
        }
        //TODO Don't hardcode this login stuff
        //There is currently a probable bug in jboss remoting, so the user realm name MUST be the same as
        //the endpoint name.
        //Connection connection = endpoint.connect(uri, OptionMap.EMPTY, "bob", endpoint.getName(), "pass".toCharArray()).get();

        IoFuture<Connection> future = endpoint.connect(uri, OptionMap.EMPTY, "bob", endpoint.getName(), "pass".toCharArray());
        Status status = future.await(2000, TimeUnit.MILLISECONDS);
        if (status == Status.WAITING) {
            future.cancel();
            throw new ConnectException("Could not connect to remote server at " + uri + " within 2 seconds");
        }
        this.connection = future.get();
        return connection;
    }

    public T openChannel(String channelName) throws IOException {
        if (connection == null) {
            throw new IllegalStateException("Not connected");
        }
        Channel channel = connection.openChannel(channelName, OptionMap.EMPTY).get();
        T wrapped = channelFactory.create(channelName, channel);
        channels.add(wrapped);
        return wrapped;
    }

    public void close() {
        for (T channel : channels) {
            try {
                channel.writeShutdown();
            } catch (IOException ignore) {
            }
//            try {
//                channel.awaitClosed();
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
        }
        channels.clear();

        IoUtils.safeClose(connection);
        if (startedEndpoint) {
            IoUtils.safeClose(providerRegistration);
            IoUtils.safeClose(endpoint);
        }
    }

    public static final class Configuration<T extends ProtocolChannel> {
        private static final AtomicInteger COUNTER = new AtomicInteger();
        private Endpoint endpoint;

        private String endpointName;
        private OptionMap optionMap = OptionMap.EMPTY;
        private ThreadGroup group;
        private String uriScheme;
        private URI uri;
        private ProtocolChannelFactory<T> channelFactory;
        private Executor executor;

        public Configuration() {
        }

        void validate() {
            if (endpointName == null && endpoint == null) {
                throw new IllegalArgumentException("Null endpoint name and null endpoing");
            }
            if (optionMap == null) {
                throw new IllegalArgumentException("Null option map");
            }
            if (uriScheme == null && endpoint == null) {
                throw new IllegalArgumentException("Null uriScheme name");
            }
            if (uriScheme != null && endpoint != null) {
                throw new IllegalArgumentException("Can't set uriScheme with specified endpoint");
            }
            if (uri == null) {
                throw new IllegalArgumentException("Null uri");
            }
            if (endpoint != null){
                //The below does not work so just hard code it for now
                if (!uri.getScheme().equals("remote")) {
                    throw new IllegalArgumentException("Only 'remote' is a valid url");
                }
                /*try {
                    endpoint.getConnectionProviderInterface(uri.getScheme(), ConnectionProviderFactory.class);
                } catch (UnknownURISchemeException e) {
                    throw new IllegalArgumentException("No " + uri.getScheme() + " registered in endpoint");
                }*/
            } else {
                if (!uriScheme.equals(uri.getScheme())) {
                    throw new IllegalArgumentException("Scheme " + uriScheme + " does not match uri " + uri);
                }
            }
            if (endpoint != null && executor != null) {
                throw new IllegalArgumentException("Don't need an executor when specified endpoint");
            }
            if (endpoint == null && executor == null) {
                throw new IllegalArgumentException("Don't need an executor when specified endpoint");
            }
            if (channelFactory == null) {
                throw new IllegalArgumentException("Null channel factory");
            }
        }

        public Endpoint getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(Endpoint endpoint) {
            this.endpoint = endpoint;
        }

        public void setEndpointName(String endpointName) {
            this.endpointName = endpointName;
        }

        public void setGroup(ThreadGroup group) {
            this.group = group;
        }

        public String getEndpointName() {
            return endpointName;
        }

        public ThreadGroup getGroup() {
            if (group == null) {
                group = new ThreadGroup("Remoting client threads " + COUNTER.incrementAndGet());
            }
            return group;
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

        public URI getUri() {
            return uri;
        }

        public void setUri(final URI uri) {
            this.uri = uri;
        }

        public Executor getExecutor() {
            return executor;
        }

        public void setExecutor(final Executor readExecutor) {
            this.executor = readExecutor;
        }

        public ProtocolChannelFactory<T> getChannelFactory() {
            return channelFactory;
        }

        public void setChannelFactory(ProtocolChannelFactory<T> channelFactory) {
            this.channelFactory = channelFactory;
        }
    }

}
