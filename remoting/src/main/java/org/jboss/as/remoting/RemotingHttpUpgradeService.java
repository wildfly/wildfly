package org.jboss.as.remoting;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedExceptionAction;


import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ChannelUpgradeHandler;
import io.undertow.server.handlers.HttpUpgradeHandshake;
import io.undertow.util.HttpString;
import org.jboss.as.controller.ServiceVerificationHandler;
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

    public static final ServiceName HTTP_UPGRADE_REGISTRY = ServiceName.JBOSS.append("management", "http-upgrade");
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("management", "remoting-http-upgrade-service");


    private final InjectedValue<ChannelUpgradeHandler> injectedRegistry = new InjectedValue<>();
    private final InjectedValue<Endpoint> injectedEndpoint = new InjectedValue<>();
    private final InjectedValue<RemotingSecurityProvider> securityProviderValue = new InjectedValue<>();
    private final OptionMap connectorPropertiesOptionMap;

    public RemotingHttpUpgradeService(final OptionMap connectorPropertiesOptionMap) {
        this.connectorPropertiesOptionMap = connectorPropertiesOptionMap;
    }

    public static void installServices(final ServiceTarget serviceTarget, final String connectorName, final ServiceName endpointName, final OptionMap connectorPropertiesOptionMap, final ServiceVerificationHandler verificationHandler) {
        final RemotingHttpUpgradeService service = new RemotingHttpUpgradeService(connectorPropertiesOptionMap);

        final ServiceName securityProviderName = RealmSecurityProviderService.createName(connectorName);

        ServiceBuilder<RemotingHttpUpgradeService> builder = serviceTarget.addService(SERVICE_NAME, service)
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .addDependency(HTTP_UPGRADE_REGISTRY, ChannelUpgradeHandler.class, service.injectedRegistry)
                .addDependency(endpointName, Endpoint.class, service.injectedEndpoint)
                .addDependency(securityProviderName, RemotingSecurityProvider.class, service.securityProviderValue);

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        builder.install();
    }



    @Override
    public void start(final StartContext context) throws StartException {
        final Endpoint endpoint = injectedEndpoint.getValue();
        RemotingSecurityProvider rsp = securityProviderValue.getValue();
        ServerAuthenticationProvider sap = rsp.getServerAuthenticationProvider();
        OptionMap.Builder builder = OptionMap.builder();
        builder.addAll(rsp.getOptionMap());

        if (connectorPropertiesOptionMap != null) {
            builder.addAll(connectorPropertiesOptionMap);
        }
        OptionMap resultingMap = builder.getMap();
        try {
            final ExternalConnectionProvider provider = endpoint.getConnectionProviderInterface("http-remoting", ExternalConnectionProvider.class);
            final ExternalConnectionProvider.ConnectionAdaptor adaptor = provider.createConnectionAdaptor(resultingMap, sap);

            injectedRegistry.getValue().addProtocol(JBOSS_REMOTING, new ChannelListener<StreamConnection>() {
                @Override
                public void handleEvent(final StreamConnection channel) {
                    if(channel instanceof SslConnection) {
                        adaptor.adapt(new AssembledConnectedSslStreamChannel((SslConnection)channel, channel.getSourceChannel(), channel.getSinkChannel()));
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
    public void stop(final StopContext context) {
        injectedRegistry.getValue().removeProtocol(JBOSS_REMOTING);
    }

    @Override
    public RemotingHttpUpgradeService getValue() throws IllegalStateException, IllegalArgumentException {
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
