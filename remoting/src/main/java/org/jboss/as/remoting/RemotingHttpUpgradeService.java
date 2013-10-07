package org.jboss.as.remoting;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.ListenerRegistry;
import io.undertow.server.handlers.ChannelUpgradeHandler;
import io.undertow.server.handlers.HttpUpgradeHandshake;
import io.undertow.util.HttpString;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedExceptionAction;
import java.util.List;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.UnknownURISchemeException;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.remoting3.spi.ExternalConnectionProvider;
import org.xnio.ChannelListener;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.channels.AssembledConnectedSslStreamChannel;
import org.xnio.channels.AssembledConnectedStreamChannel;
import org.xnio.channels.SslConnection;

/**
 * Service that registers a HTTP upgrade handler to enable remoting to be used via http upgrade.
 *
 * @author Stuart Douglas
 */
public class RemotingHttpUpgradeService implements Service<RemotingHttpUpgradeService> {


    public static final String JBOSS_REMOTING = "jboss-remoting";

    /**
     * Base service name for this HTTP Upgrade refist
     */
    public static final ServiceName HTTP_UPGRADE_REGISTRY = ServiceName.JBOSS.append("http-upgrade-registry");
    public static final ServiceName UPGRADE_SERVICE_NAME = ServiceName.JBOSS.append("remoting ", "remoting-http-upgrade-service");

    private final String httpConnectorName;
    private final String endpointName;


    private final InjectedValue<ChannelUpgradeHandler> injectedRegistry = new InjectedValue<>();
    private final InjectedValue<ListenerRegistry> listenerRegistry = new InjectedValue<>();
    private final InjectedValue<Endpoint> injectedEndpoint = new InjectedValue<>();
    private final InjectedValue<RemotingSecurityProvider> securityProviderValue = new InjectedValue<>();
    private final OptionMap connectorPropertiesOptionMap;

    private ListenerRegistry.HttpUpgradeMetadata httpUpgradeMetadata;

    public RemotingHttpUpgradeService(final String httpConnectorName, final String endpointName, final OptionMap connectorPropertiesOptionMap) {
        this.httpConnectorName = httpConnectorName;
        this.endpointName = endpointName;
        this.connectorPropertiesOptionMap = connectorPropertiesOptionMap;
    }


    public static void installServices(final ServiceTarget serviceTarget, final String remotingConnectorName, final String httpConnectorName, final ServiceName endpointName, final OptionMap connectorPropertiesOptionMap, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {
        final RemotingHttpUpgradeService service = new RemotingHttpUpgradeService(httpConnectorName, endpointName.getSimpleName(), connectorPropertiesOptionMap);

        final ServiceName securityProviderName = RealmSecurityProviderService.createName(remotingConnectorName);

        ServiceBuilder<RemotingHttpUpgradeService> builder = serviceTarget.addService(UPGRADE_SERVICE_NAME.append(remotingConnectorName), service)
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .addDependency(HTTP_UPGRADE_REGISTRY.append(httpConnectorName), ChannelUpgradeHandler.class, service.injectedRegistry)
                .addDependency(HttpListenerRegistryService.SERVICE_NAME, ListenerRegistry.class, service.listenerRegistry)
                .addDependency(endpointName, Endpoint.class, service.injectedEndpoint)
                .addDependency(securityProviderName, RemotingSecurityProvider.class, service.securityProviderValue);

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        ServiceController<RemotingHttpUpgradeService> controller = builder.install();
        if(newControllers != null) {
            newControllers.add(controller);
        }
    }


    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final Endpoint endpoint = injectedEndpoint.getValue();
        RemotingSecurityProvider rsp = securityProviderValue.getValue();
        ServerAuthenticationProvider sap = rsp.getServerAuthenticationProvider();
        OptionMap.Builder builder = OptionMap.builder();
        builder.addAll(rsp.getOptionMap());

        ListenerRegistry.Listener listenerInfo = listenerRegistry.getValue().getListener(httpConnectorName);
        assert listenerInfo != null;
        listenerInfo.addHttpUpgradeMetadata(httpUpgradeMetadata = new ListenerRegistry.HttpUpgradeMetadata("jboss-remoting", endpointName));
        RemotingConnectorBindingInfoService.install(context.getChildTarget(), context.getController().getName().getSimpleName(), (SocketBinding)listenerInfo.getContextInformation("socket-binding"), listenerInfo.getProtocol().equals("https") ? "https-remoting" : "http-remoting");

        if (connectorPropertiesOptionMap != null) {
            builder.addAll(connectorPropertiesOptionMap);
        }
        OptionMap resultingMap = builder.getMap();
        try {
            final ExternalConnectionProvider provider = endpoint.getConnectionProviderInterface(Protocol.HTTP_REMOTING.toString(), ExternalConnectionProvider.class);
            final ExternalConnectionProvider.ConnectionAdaptor adaptor = provider.createConnectionAdaptor(resultingMap, sap);

            injectedRegistry.getValue().addProtocol(JBOSS_REMOTING, new ChannelListener<StreamConnection>() {
                @Override
                public void handleEvent(final StreamConnection channel) {
                    if (channel instanceof SslConnection) {
                        adaptor.adapt(new AssembledConnectedSslStreamChannel((SslConnection) channel, channel.getSourceChannel(), channel.getSinkChannel()));
                    } else {
                        adaptor.adapt(new AssembledConnectedStreamChannel(channel, channel.getSourceChannel(), channel.getSinkChannel()));
                    }
                }
            }, new RemotingUpgradeHanshake());

        } catch (UnknownURISchemeException e) {
            throw new StartException(e);
        } catch (IOException e) {
            throw new StartException(e);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        listenerRegistry.getValue().getListener(httpConnectorName).removeHttpUpgradeMetadata(httpUpgradeMetadata);
        httpUpgradeMetadata = null;
        injectedRegistry.getValue().removeProtocol(JBOSS_REMOTING);
    }

    @Override
    public synchronized RemotingHttpUpgradeService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private static final class RemotingUpgradeHanshake implements HttpUpgradeHandshake {


        /**
         * Magic number used in the handshake.
         * <p/>
         * The handshake borrows heavily from the web socket protocol, but uses different header
         * names and a different magic number.
         */
        public static final String MAGIC_NUMBER = "CF70DEB8-70F9-4FBA-8B4F-DFC3E723B4CD";

        //headers
        public static final HttpString SEC_JBOSS_REMOTING_KEY = new HttpString("Sec-JbossRemoting-Key");
        public static final HttpString SEC_JBOSS_REMOTING_ACCEPT = new HttpString("Sec-JbossRemoting-Accept");

        @Override
        public boolean handleUpgrade(final HttpServerExchange exchange) throws IOException {
            String key = exchange.getRequestHeaders().getFirst(SEC_JBOSS_REMOTING_KEY);
            if (key == null) {
                throw RemotingMessages.MESSAGES.upgradeRequestMissingKey();
            }
            exchange.getResponseHeaders().put(SEC_JBOSS_REMOTING_ACCEPT, createExpectedResponse(key));
            return true;
        }

        protected String createExpectedResponse(String secKey) throws IOException {
            try {
                final String concat = secKey + MAGIC_NUMBER;
                final MessageDigest digest = MessageDigest.getInstance("SHA1");

                digest.update(concat.getBytes("UTF-8"));
                final byte[] bytes = digest.digest();
                return FlexBase64.encodeString(bytes, false);
            } catch (NoSuchAlgorithmException e) {
                throw new IOException(e);
            }

        }


        private static class FlexBase64 {
            /*
             * Note that this code heavily favors performance over reuse and clean style.
             */

            private static final byte[] ENCODING_TABLE;
            private static final byte[] DECODING_TABLE = new byte[80];
            private static final Constructor<String> STRING_CONSTRUCTOR;

            static {
                try {
                    ENCODING_TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes("ASCII");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException();
                }

                for (int i = 0; i < ENCODING_TABLE.length; i++) {
                    int v = (ENCODING_TABLE[i] & 0xFF) - 43;
                    DECODING_TABLE[v] = (byte) (i + 1);  // zero = illegal
                }

                Constructor<String> c = null;
                try {
                    PrivilegedExceptionAction<Constructor<String>> runnable = new PrivilegedExceptionAction<Constructor<String>>() {
                        @Override
                        public Constructor<String> run() throws Exception {
                            Constructor<String> c;
                            c = String.class.getDeclaredConstructor(char[].class, boolean.class);
                            c.setAccessible(true);
                            return c;
                        }
                    };
                    if (System.getSecurityManager() != null) {
                        c = AccessController.doPrivileged(runnable);
                    } else {
                        c = runnable.run();
                    }
                } catch (Throwable t) {
                }

                STRING_CONSTRUCTOR = c;
            }

            /**
             * Encodes a fixed and complete byte array into a Base64 String.
             *
             * @param source the byte array to encode from
             * @param wrap   whether or not to wrap the output at 76 chars with CRLFs
             * @return a new String representing the Base64 output
             */
            public static String encodeString(byte[] source, boolean wrap) {
                return encodeString(source, 0, source.length, wrap);
            }


            private static String encodeString(byte[] source, int pos, int limit, boolean wrap) {
                int olimit = (limit - pos);
                int remainder = olimit % 3;
                olimit = (olimit + (remainder == 0 ? 0 : 3 - remainder)) / 3 * 4;
                olimit += (wrap ? (olimit / 76) * 2 + 2 : 0);
                char[] target = new char[olimit];
                int opos = 0;
                int last = 0;
                int count = 0;
                int state = 0;
                final byte[] ENCODING_TABLE = FlexBase64.ENCODING_TABLE;

                while (limit > pos) {
                    //  ( 6 | 2) (4 | 4) (2 | 6)
                    int b = source[pos++] & 0xFF;
                    target[opos++] = (char) ENCODING_TABLE[b >>> 2];
                    last = (b & 0x3) << 4;
                    if (pos >= limit) {
                        state = 1;
                        break;
                    }
                    b = source[pos++] & 0xFF;
                    target[opos++] = (char) ENCODING_TABLE[last | (b >>> 4)];
                    last = (b & 0x0F) << 2;
                    if (pos >= limit) {
                        state = 2;
                        break;
                    }
                    b = source[pos++] & 0xFF;
                    target[opos++] = (char) ENCODING_TABLE[last | (b >>> 6)];
                    target[opos++] = (char) ENCODING_TABLE[b & 0x3F];

                    if (wrap) {
                        count += 4;
                        if (count >= 76) {
                            count = 0;
                            target[opos++] = 0x0D;
                            target[opos++] = 0x0A;
                        }
                    }
                }

                complete(target, opos, state, last, wrap);

                try {
                    // Eliminate copying on Open/Oracle JDK
                    if (STRING_CONSTRUCTOR != null) {
                        return STRING_CONSTRUCTOR.newInstance(target, Boolean.TRUE);
                    }
                } catch (Exception e) {
                }

                return new String(target);
            }

            private static int complete(char[] target, int pos, int state, int last, boolean wrap) {
                if (state > 0) {
                    target[pos++] = (char) ENCODING_TABLE[last];
                    for (int i = state; i < 3; i++) {
                        target[pos++] = '=';
                    }
                }
                if (wrap) {
                    target[pos++] = 0x0D;
                    target[pos++] = 0x0A;
                }

                return pos;
            }

        }

    }

}
