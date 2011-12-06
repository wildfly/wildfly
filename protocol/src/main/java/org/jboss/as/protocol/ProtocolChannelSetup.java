/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.IoFuture;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import static org.xnio.Options.SASL_POLICY_NOANONYMOUS;
import static org.xnio.Options.SASL_POLICY_NOPLAINTEXT;
import org.xnio.Property;
import org.xnio.Sequence;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Emanuel Muckenhuber
 */
public class ProtocolChannelSetup implements Closeable {

    private static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";
    private final boolean startedEndpoint;
    private final Endpoint endpoint;
    private final Registration providerRegistration;
    private final URI uri;

    private ProtocolChannelSetup(final boolean startedEndpoint, final Endpoint endpoint,
                                 final Registration providerRegistration, final URI uri) {

        this.startedEndpoint = startedEndpoint;
        this.endpoint = endpoint;
        this.providerRegistration = providerRegistration;
        this.uri = uri;
    }

    public static ProtocolChannelSetup create(final Configuration configuration) throws IOException {
        if (configuration == null) {
            throw MESSAGES.nullVar("configuration");
        }
        configuration.validate();

        final Endpoint endpoint;
        if (configuration.getEndpoint() != null) {
            endpoint = configuration.getEndpoint();
            return new ProtocolChannelSetup(false, endpoint, null, configuration.getUri());
        } else {
            endpoint = Remoting.createEndpoint(configuration.getEndpointName(), configuration.getOptionMap());
            Registration providerRegistration = endpoint.addConnectionProvider(configuration.getUri().getScheme(), new RemoteConnectionProviderFactory(), OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE));
            return new ProtocolChannelSetup(true, endpoint, providerRegistration, configuration.getUri());
        }
    }

    public IoFuture<Connection> connect(CallbackHandler handler) throws IOException {
        return connect(handler, null);
    }

    public IoFuture<Connection> connect(CallbackHandler handler, Map<String, String> saslOptions) throws IOException {

        OptionMap.Builder builder = OptionMap.builder();
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
        return endpoint.connect(uri, builder.getMap(), wrapperHandler);
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

    public void close() {
        if (startedEndpoint) {
            IoUtils.safeClose(providerRegistration);
            IoUtils.safeClose(endpoint);
        }
    }

    public static final class Configuration {
        private static final long DEFAULT_CONNECT_TIMEOUT = 5000;

        private static final AtomicInteger COUNTER = new AtomicInteger();
        private Endpoint endpoint;

        private String endpointName;
        private OptionMap optionMap = OptionMap.EMPTY;
        private ThreadGroup group;
        private String uriScheme;
        private URI uri;

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
            } else {
                if (!uriScheme.equals(uri.getScheme())) {
                    throw MESSAGES.unmatchedScheme(uriScheme, uri);
                }
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
