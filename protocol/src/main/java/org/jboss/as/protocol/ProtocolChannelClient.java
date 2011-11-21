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

import static org.jboss.as.protocol.ProtocolMessages.MESSAGES;
import static org.xnio.Options.SASL_POLICY_NOANONYMOUS;
import static org.xnio.Options.SASL_POLICY_NOPLAINTEXT;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.IoFuture;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Property;
import org.xnio.Sequence;
import org.xnio.OptionMap.Builder;

/**
 * This class is not thread safe and should only be used by one thread
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ProtocolChannelClient<T extends ProtocolChannel> implements Closeable {
    private static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";
    private final boolean startedEndpoint;
    private final Endpoint endpoint;
    private final Registration providerRegistration;
    private final URI uri;
    private final ProtocolChannelFactory<T> channelFactory;
    private volatile Connection connection;
    private final Set<T> channels = new HashSet<T>();

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
            throw MESSAGES.nullVar("configuration");
        }
        configuration.validate();

        final Endpoint endpoint;
        if (configuration.getEndpoint() != null) {
            endpoint = configuration.getEndpoint();
            return new ProtocolChannelClient<T>(false, endpoint, null, configuration.getUri(), configuration.getChannelFactory());
        } else {
            endpoint = Remoting.createEndpoint(configuration.getEndpointName(), configuration.getOptionMap());
            Registration providerRegistration = endpoint.addConnectionProvider(configuration.getUri().getScheme(), new RemoteConnectionProviderFactory(), OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE));
            return new ProtocolChannelClient<T>(true, endpoint, providerRegistration, configuration.getUri(), configuration.getChannelFactory());
        }
    }


    public Connection connect(CallbackHandler handler) throws IOException {
        return connect(handler, null);
    }

    public Connection connect(CallbackHandler handler, Map<String, String> saslOptions) throws IOException {
        if (connection != null) {
            throw MESSAGES.alreadyConnected();
        }

        Builder builder = OptionMap.builder();
        builder.set(SASL_POLICY_NOANONYMOUS, Boolean.FALSE);
        builder.set(SASL_POLICY_NOPLAINTEXT, Boolean.FALSE);
        if (isLocal() == false) {
            builder.set(Options.SASL_DISALLOWED_MECHANISMS, Sequence.of(JBOSS_LOCAL_USER));
        }
        List<Property> tempProperties = new ArrayList<Property>(saslOptions != null ? saslOptions.size() : 1);
        tempProperties.add(Property.of("jboss.sasl.local-user.quiet-auth", "true"));
        if (saslOptions != null) {
            for (String currentKey : saslOptions.keySet()) {
                tempProperties.add(Property.of(currentKey, saslOptions.get(currentKey)));
            }
        }
        builder.set(Options.SASL_PROPERTIES, Sequence.of(tempProperties));

        CallbackHandler actualHandler = handler != null ? handler : new AnonymousCallbackHandler();
        WrapperCallbackHandler wrapperHandler = new WrapperCallbackHandler(actualHandler);
        IoFuture<Connection> future = endpoint.connect(uri, builder.getMap(), wrapperHandler);
        try {
            this.connection = future.get();
        } catch (CancellationException e) {
            throw MESSAGES.connectWasCancelled();
        } catch (IOException e) {
            throw MESSAGES.couldNotConnect(uri, e);
        }

        return connection;
    }

    private boolean isLocal() {
        try {
            String hostName = uri.getHost();
            InetAddress address = InetAddress.getByName(hostName);
            NetworkInterface nic = NetworkInterface.getByInetAddress(address);

            return address.isLoopbackAddress() || nic != null;
        } catch (Exception e) {
            return false;
        }
    }

    public T openChannel(String channelName) throws IOException {
        if (connection == null) {
            throw MESSAGES.notConnected();
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
        private static final long DEFAULT_CONNECT_TIMEOUT = 5000;

        private static final AtomicInteger COUNTER = new AtomicInteger();
        private Endpoint endpoint;

        private String endpointName;
        private OptionMap optionMap = OptionMap.EMPTY;
        private ThreadGroup group;
        private String uriScheme;
        private URI uri;
        private ProtocolChannelFactory<T> channelFactory;

        //Flags to avoid spamming logs with warnings every time someone tries to set these
        private static volatile boolean warnedExecutor;
        private static volatile boolean warnedConnectTimeout;
        private static volatile boolean warnedConnectTimeoutProperty;

        public Configuration() {
        }

        void validate() {
            if (endpointName == null && endpoint == null) {
                throw MESSAGES.nullParameters("endpointName", "endpoint");
            }
            if (optionMap == null) {
                throw MESSAGES.nullVar("optionMap");
            }
            if (uriScheme == null && endpoint == null) {
                throw MESSAGES.nullVar("uriScheme");
            }
            if (uriScheme != null && endpoint != null) {
                throw MESSAGES.cannotSetUriScheme();
            }
            if (uri == null) {
                throw MESSAGES.nullVar("uri");
            }
            if (endpoint != null){
                //The below does not work so just hard code it for now
                if (!uri.getScheme().equals("remote")) {
                    throw MESSAGES.invalidUrl("remote");
                }
                /*try {
                    endpoint.getConnectionProviderInterface(uri.getScheme(), ConnectionProviderFactory.class);
                } catch (UnknownURISchemeException e) {
                    throw new IllegalArgumentException("No " + uri.getScheme() + " registered in endpoint");
                }*/
            } else {
                if (!uriScheme.equals(uri.getScheme())) {
                    throw MESSAGES.unmatchedScheme(uriScheme, uri);
                }
            }
            if (channelFactory == null) {
                throw MESSAGES.nullVar("channelFactory");
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

        public void setConnectTimeoutProperty(String connectTimeoutProperty) {
            boolean warned = warnedConnectTimeoutProperty;
            if (!warned) {
                warnedConnectTimeoutProperty = true;
                ProtocolLogger.CLIENT_LOGGER.connectTimeoutPropertyNotNeeded();
            }
        }

        public void setConnectTimeout(long connectTimeout) {
            boolean warned = warnedConnectTimeout;
            if (!warned) {
                warnedConnectTimeout = true;
                ProtocolLogger.CLIENT_LOGGER.connectTimeoutNotNeeded();
            }
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

        /**
         * @deprecated The executor is no longer needed. Here for backwards compatibility
         */
        @Deprecated
        public void setExecutor(final Executor readExecutor) {
           boolean warned = warnedExecutor;
           if (!warned) {
               warnedExecutor = true;
               ProtocolLogger.CLIENT_LOGGER.executorNotNeeded();
           }
        }

        public ProtocolChannelFactory<T> getChannelFactory() {
            return channelFactory;
        }

        public void setChannelFactory(ProtocolChannelFactory<T> channelFactory) {
            this.channelFactory = channelFactory;
        }
    }

    private static final class WrapperCallbackHandler implements CallbackHandler {

        private volatile boolean inCall = false;

        private volatile long callFinished = -1;

        private final CallbackHandler wrapped;

        WrapperCallbackHandler(final CallbackHandler toWrap) {
            this.wrapped = toWrap;
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            inCall = true;
            try {
                wrapped.handle(callbacks);
            } finally {
                // Set the time first so if a read is made between these two calls it will say inCall=true until
                // callFinished is set.
                callFinished = System.currentTimeMillis();
                inCall = false;
            }
        }

        boolean isInCall() {
            return inCall;
        }

        long getCallFinished() {
            return callFinished;
        }
    }

    private static final class AnonymousCallbackHandler implements CallbackHandler {

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback current : callbacks) {
                if (current instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) current;
                    ncb.setName("anonymous");
                } else {
                    throw new UnsupportedCallbackException(current);
                }
            }
        }

    }

}
