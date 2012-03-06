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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

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

/**
 * This class is not thread safe and should only be used by one thread
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ProtocolChannelClient implements Closeable {

    private static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";

    private final Endpoint endpoint;
    private final Configuration configuration;
    private final URI uri;

    private ProtocolChannelClient(final Endpoint endpoint, final Configuration configuration) {

        this.endpoint = endpoint;
        this.configuration = configuration;
        this.uri = configuration.getUri();
    }

    public static ProtocolChannelClient create(final Configuration configuration) throws IOException {
        if (configuration == null) {
            throw MESSAGES.nullVar("configuration");
        }
        configuration.validate();

        final Endpoint endpoint = configuration.getEndpoint();
        return new ProtocolChannelClient(endpoint, configuration);
    }

    public IoFuture<Connection> connect(CallbackHandler handler) throws IOException {
        return connect(handler, null, null);
    }

    public IoFuture<Connection> connect(CallbackHandler handler, Map<String, String> saslOptions, SSLContext sslContext) throws IOException {
        OptionMap.Builder builder = OptionMap.builder();
        builder.addAll(configuration.getOptionMap());
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

        builder.set(Options.SSL_ENABLED, true);
        builder.set(Options.SSL_STARTTLS, true);

        CallbackHandler actualHandler = handler != null ? handler : new AnonymousCallbackHandler();
        return endpoint.connect(uri, builder.getMap(), actualHandler, sslContext);
    }

    public Connection connectSync(CallbackHandler handler) throws IOException {
        return connectSync(handler, null, null);
    }

    public Connection connectSync(CallbackHandler handler, Map<String, String> saslOptions, SSLContext sslContext) throws IOException {
        WrapperCallbackHandler wrapperHandler = new WrapperCallbackHandler(handler);
        final IoFuture<Connection> future = connect(wrapperHandler, saslOptions, sslContext);
        long timeoutMillis = configuration.getConnectionTimeout();
        IoFuture.Status status = future.await(timeoutMillis, TimeUnit.MILLISECONDS);
        while (status == IoFuture.Status.WAITING) {
            if (wrapperHandler.isInCall()) {
                // If there is currently an interaction with the user just wait again.
                status = future.await(timeoutMillis, TimeUnit.MILLISECONDS);
            } else {
                long lastInteraction = wrapperHandler.getCallFinished();
                if (lastInteraction > 0) {
                    long now = System.currentTimeMillis();
                    long timeSinceLast = now - lastInteraction;
                    if (timeSinceLast < timeoutMillis) {
                        // As this point we are setting the timeout based on the time of the last interaction
                        // with the user, if there is any time left we will wait for that time but dont wait for
                        // a full timeout.
                        status = future.await(timeoutMillis - timeSinceLast, TimeUnit.MILLISECONDS);
                    } else {
                        status = null;
                    }
                } else {
                    status = null; // Just terminate status processing.
                }
            }
        }

        if (status == IoFuture.Status.DONE) {
            return future.get();
        }
        if (status == IoFuture.Status.FAILED) {
            throw ProtocolMessages.MESSAGES.failedToConnect(uri, future.getException());
        }
        throw ProtocolMessages.MESSAGES.couldNotConnect(uri);
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
        //
    }

    public static final class Configuration {
        private static final long DEFAULT_CONNECT_TIMEOUT = 5000;

        private URI uri;
        private Endpoint endpoint;
        private OptionMap optionMap = OptionMap.EMPTY;
        private long connectionTimeout = DEFAULT_CONNECT_TIMEOUT;

        //Flags to avoid spamming logs with warnings every time someone tries to set these
        private static volatile boolean warnedExecutor;
        private static volatile boolean warnedConnectTimeout;
        private static volatile boolean warnedConnectTimeoutProperty;

        public Configuration() {
        }

        void validate() {
            if (endpoint == null) {
                throw MESSAGES.nullVar("endpoint");
            }
            if (optionMap == null) {
                throw MESSAGES.nullVar("optionMap");
            }
            if (uri == null) {
                throw MESSAGES.nullVar("uri");
            }
        }

        public Endpoint getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(Endpoint endpoint) {
            this.endpoint = endpoint;
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

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
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
