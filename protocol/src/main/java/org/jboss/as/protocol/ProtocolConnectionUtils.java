/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.xnio.IoFuture;
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
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Protocol Connection utils.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ProtocolConnectionUtils {

    private static final String JBOSS_LOCAL_USER = "JBOSS-LOCAL-USER";

    /**
     * Connect.
     *
     * @param configuration the connection configuration
     * @return the future connection
     * @throws IOException
     */
    public static IoFuture<Connection> connect(final ProtocolConnectionConfiguration configuration) throws IOException {
        return connect(configuration.getCallbackHandler(), configuration);
    }

    private static IoFuture<Connection> connect(final CallbackHandler handler, final ProtocolConnectionConfiguration configuration) throws IOException {
        final Endpoint endpoint = configuration.getEndpoint();
        final OptionMap options = getOptions(configuration);
        final CallbackHandler actualHandler = handler != null ? handler : new AnonymousCallbackHandler();
        return endpoint.connect(configuration.getUri(), options, actualHandler, configuration.getSslContext());
    }

    /**
     * Connect sync.
     *
     * @param configuration the protocol configuration
     * @return the connection
     * @throws IOException
     */
    public static Connection connectSync(final ProtocolConnectionConfiguration configuration) throws IOException {
        final CallbackHandler handler = configuration.getCallbackHandler();
        final CallbackHandler actualHandler = handler != null ? handler : new AnonymousCallbackHandler();
        final WrapperCallbackHandler wrapperHandler = new WrapperCallbackHandler(actualHandler);
        final IoFuture<Connection> future = connect(wrapperHandler, configuration);
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
            throw ProtocolMessages.MESSAGES.failedToConnect(configuration.getUri(), future.getException());
        }
        throw ProtocolMessages.MESSAGES.couldNotConnect(configuration.getUri());
    }

    private static OptionMap getOptions(final ProtocolConnectionConfiguration configuration) {
        final Map<String, String> saslOptions = configuration.getSaslOptions();
        final OptionMap.Builder builder = OptionMap.builder();
        builder.addAll(configuration.getOptionMap());
        builder.set(SASL_POLICY_NOANONYMOUS, Boolean.FALSE);
        builder.set(SASL_POLICY_NOPLAINTEXT, Boolean.FALSE);
        if (isLocal(configuration.getUri()) == false) {
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

        return builder.getMap();
    }

    private static boolean isLocal(final URI uri) {
        try {
            final String hostName = uri.getHost();
            final InetAddress address = InetAddress.getByName(hostName);
            final NetworkInterface nic = NetworkInterface.getByInetAddress(address);
            return address.isLoopbackAddress() || nic != null;
        } catch (Exception e) {
            return false;
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
