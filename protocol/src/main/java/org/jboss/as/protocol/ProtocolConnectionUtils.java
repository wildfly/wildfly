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
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

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

        String clientBindAddress = configuration.getClientBindAddress();
        if (clientBindAddress == null) {
            return endpoint.connect(configuration.getUri(), options, actualHandler, configuration.getSslContext());
        } else {
            InetSocketAddress bindAddr = new InetSocketAddress(clientBindAddress, 0);
            InetSocketAddress destAddr = new InetSocketAddress(configuration.getUri().getHost(), configuration.getUri().getPort());
            return endpoint.connect(configuration.getUri().getScheme(), bindAddr, destAddr, options, actualHandler, configuration.getSslContext());
        }
    }

    /**
     * Connect sync.
     *
     * @param configuration the protocol configuration
     * @return the connection
     * @throws IOException
     */
    public static Connection connectSync(final ProtocolConnectionConfiguration configuration) throws IOException {
        long timeoutMillis = configuration.getConnectionTimeout();
        CallbackHandler handler = configuration.getCallbackHandler();
        final CallbackHandler actualHandler;
        ProtocolTimeoutHandler timeoutHandler = configuration.getTimeoutHandler();
        // Note: If a client supplies a ProtocolTimeoutHandler it is taking on full responsibility for timeout management.
        if (timeoutHandler == null) {
            GeneralTimeoutHandler defaultTimeoutHandler = new GeneralTimeoutHandler();
            // No point wrapping our AnonymousCallbackHandler.
            actualHandler = handler != null ? new WrapperCallbackHandler(defaultTimeoutHandler, handler)
                    : new AnonymousCallbackHandler();
            timeoutHandler = defaultTimeoutHandler;
        } else {
            actualHandler = handler != null ? handler : new AnonymousCallbackHandler();
        }

        final IoFuture<Connection> future = connect(actualHandler, configuration);

        IoFuture.Status status = timeoutHandler.await(future, timeoutMillis);

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
        builder.set(SASL_POLICY_NOANONYMOUS, Boolean.FALSE);
        builder.set(SASL_POLICY_NOPLAINTEXT, Boolean.FALSE);
        builder.addAll(configuration.getOptionMap());
        configureSaslMechnisms(saslOptions, isLocal(configuration.getUri()), builder);
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

    private static void configureSaslMechnisms(Map<String, String> saslOptions, boolean isLocal, OptionMap.Builder builder) {
        String[] mechanisms = null;
        String listed;
        if (saslOptions != null && (listed = saslOptions.get(Options.SASL_DISALLOWED_MECHANISMS.getName())) != null) {
            // Disallowed mechanisms were passed via the saslOptions map; need to convert to an XNIO option
            String[] split = listed.split(" ");
            if (isLocal) {
                mechanisms = new String[split.length + 1];
                mechanisms[0] = JBOSS_LOCAL_USER;
                System.arraycopy(split, 0, mechanisms, 1, split.length);
            } else {
                mechanisms = split;
            }
        } else if (!isLocal) {
            mechanisms = new String[]{ JBOSS_LOCAL_USER };
        }

        if (mechanisms != null) {
            builder.set(Options.SASL_DISALLOWED_MECHANISMS, Sequence.of(mechanisms));
        }

        if (saslOptions != null && (listed = saslOptions.get(Options.SASL_MECHANISMS.getName())) != null) {
            // SASL mechanisms were passed via the saslOptions map; need to convert to an XNIO option
            String[] split = listed.split(" ");
            if (split.length > 0) {
                builder.set(Options.SASL_MECHANISMS, Sequence.of(split));
            }
        }
    }

    private static boolean isLocal(final URI uri) {
        try {
            final String hostName = uri.getHost();
            final InetAddress address = InetAddress.getByName(hostName);
            NetworkInterface nic;
            if (address.isLinkLocalAddress()) {
                /*
                 * AS7-6382 On Windows the getByInetAddress was not identifying a NIC where the address did not have the zone
                 * ID, this manual iteration does allow for the address to be matched.
                 */
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                nic = null;
                while (interfaces.hasMoreElements() && nic == null) {
                    NetworkInterface current = interfaces.nextElement();
                    Enumeration<InetAddress> addresses = current.getInetAddresses();
                    while (addresses.hasMoreElements() && nic == null) {
                        InetAddress currentAddress = addresses.nextElement();
                        if (address.equals(currentAddress)) {
                            nic = current;
                        }
                    }
                }
            } else {
                nic = NetworkInterface.getByInetAddress(address);
            }
            return address.isLoopbackAddress() || nic != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static final class WrapperCallbackHandler implements CallbackHandler {

        private final GeneralTimeoutHandler timeoutHandler;
        private final CallbackHandler wrapped;

        WrapperCallbackHandler(final GeneralTimeoutHandler timeoutHandler, final CallbackHandler toWrap) {
            this.timeoutHandler = timeoutHandler;
            this.wrapped = toWrap;
        }

        public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {

            try {
                timeoutHandler.suspendAndExecute(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            wrapped.handle(callbacks);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (UnsupportedCallbackException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                } else if (e.getCause() instanceof UnsupportedCallbackException) {
                    throw (UnsupportedCallbackException) e.getCause();
                }
                throw e;
            }
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
