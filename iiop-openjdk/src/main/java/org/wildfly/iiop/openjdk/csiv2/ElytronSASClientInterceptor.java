/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.iiop.openjdk.csiv2;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.Principal;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.sun.corba.se.impl.interceptors.ClientRequestInfoImpl;
import com.sun.corba.se.impl.transport.SocketOrChannelContactInfoImpl;
import com.sun.corba.se.pept.transport.ContactInfo;
import com.sun.corba.se.spi.transport.CorbaConnection;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CSI.AuthorizationElement;
import org.omg.CSI.EstablishContext;
import org.omg.CSI.GSS_NT_ExportedNameHelper;
import org.omg.CSI.ITTAnonymous;
import org.omg.CSI.IdentityToken;
import org.omg.CSI.MTContextError;
import org.omg.CSI.SASContextBody;
import org.omg.CSI.SASContextBodyHelper;
import org.omg.CSIIOP.CompoundSecMech;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.IdentityAssertion;
import org.omg.GSSUP.InitialContextToken;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ForwardRequest;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.AuthenticationContextConfigurationClient;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.auth.principal.AnonymousPrincipal;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * This implementation of {@code org.omg.PortableInterceptor.ClientRequestInterceptor} inserts the security attribute
 * service (SAS) context into outgoing IIOP requests and handles the SAS messages received from the target security
 * service in the SAS context of incoming IIOP replies.
 * <p/>
 * When creating the SAS context, this implementation looks for an Elytron {@link AuthenticationConfiguration} that matches
 * the target URI (in the form iiop://hostname:port) and then uses the configuration to obtain the security info (like
 * username and password) that is inserted into the security tokens that are set in the SAS context.
 * <p/>
 * The type of security tokens that are constructed depends on the target security requirements:
 * <ul>
 *     <li>
 *         If the target supports identity propagation, the identity obtained from the Elytron configuration that matches
 *         the target URI to build the {@link IdentityToken} that is inserted into the SAS context. This usually means using
 *         a configuration backed by a security domain so that the current authenticated identity in that domain is used
 *         to build the identity token.
 *     </li>
 *     <li>
 *         If in addition to the identity token the target requires username/password authentication, it means the target
 *         expects this runtime (server) to identify itself using its own username and credentials. Once this runtime
 *         has been authenticated, the identity contained in the identity token is used as a run-as identity.
 *         <p/>
 *         In terms of configuration, it must match the target URI and it is usually a config that defines this
 *         server's auth-name and associated credential via credential-reference.
 *     </li>
 *     <li>
 *         If the target doesn't support identity propagation but supports username/password authentication, the identity
 *         and credentials obtained from the Elytron configuration that matches the target URI to build
 *         the {@link InitialContextToken}. Again, this usually means using a configuration backed by a security domain so
 *         that the current authenticated identity in that domain and its associated credentials are used to build the
 *         initial context token.
 *     </li>
 * </ul>
 */
public class ElytronSASClientInterceptor extends LocalObject implements ClientRequestInterceptor {

    private static final int SAS_CONTEXT_ID = org.omg.IOP.SecurityAttributeService.value;

    private static final String AUTHENTICATION_CONTEXT_CAPABILITY =  "org.wildfly.security.authentication-context";

    private static final RuntimeCapability<Void> AUTHENTICATION_CONTEXT_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(AUTHENTICATION_CONTEXT_CAPABILITY, true, AuthenticationContext.class)
            .build();

    private static final AuthenticationContextConfigurationClient AUTH_CONFIG_CLIENT =
            AccessController.doPrivileged(AuthenticationContextConfigurationClient.ACTION);

    private static final IdentityToken ABSENT_IDENTITY_TOKEN;

    static {
        ABSENT_IDENTITY_TOKEN = new IdentityToken();
        ABSENT_IDENTITY_TOKEN.absent(true);
    }

    private static final byte[] NO_AUTHENTICATION_TOKEN = {};

    private static final AuthorizationElement[] NO_AUTHORIZATION_TOKEN = {};

    private static String authenticationContextName;

    public static void setAuthenticationContextName(final String authenticationContextName) {
        ElytronSASClientInterceptor.authenticationContextName = authenticationContextName;
    }

    private Codec codec;

    private AuthenticationContext authContext;

    public ElytronSASClientInterceptor(final Codec codec) {
        this.codec = codec;

        // initialize the authentication context.
        final ServiceContainer container = this.currentServiceContainer();
        if(authenticationContextName != null) {
            final ServiceName authContextServiceName = AUTHENTICATION_CONTEXT_RUNTIME_CAPABILITY.getCapabilityServiceName(authenticationContextName);
            this.authContext = (AuthenticationContext) container.getRequiredService(authContextServiceName).getValue();
        } else {
            this.authContext = null;
        }
    }

    @Override
    public void send_request(ClientRequestInfo ri) throws ForwardRequest {
        try {
            CompoundSecMech secMech = CSIv2Util.getMatchingSecurityMech(ri, codec,
                    EstablishTrustInClient.value,  /* client supports */
                    (short) 0                       /* client requires */);
            if (secMech == null) {
                return;
            }

            // these "null tokens" will be changed if needed.
            IdentityToken identityToken = ABSENT_IDENTITY_TOKEN;
            byte[] encodedAuthenticationToken = NO_AUTHENTICATION_TOKEN;
            final URI uri = this.getURI(ri);
            if(uri == null) {
                return;
            }
            SecurityDomain domain = SecurityDomain.getCurrent();
            SecurityIdentity currentIdentity = null;
            if(domain != null) {
                currentIdentity = domain.getCurrentSecurityIdentity();
            }
            final AuthenticationContext authContext;
            if(this.authContext != null) {
                authContext = this.authContext;
            } else if(currentIdentity == null || currentIdentity.isAnonymous()) {
                authContext = AuthenticationContext.captureCurrent();
            } else {
                authContext = AuthenticationContext.empty().with(MatchRule.ALL, AuthenticationConfiguration.empty().useForwardedIdentity(domain));
            }

            if ((secMech.sas_context_mech.target_supports & IdentityAssertion.value) != 0) {
                final AuthenticationConfiguration configuration = AUTH_CONFIG_CLIENT.getAuthenticationConfiguration(uri, authContext, -1, null, null);
                final Principal principal = AUTH_CONFIG_CLIENT.getPrincipal(configuration);

                if (principal != null && principal != AnonymousPrincipal.getInstance()) {
                    // The name scope needs to be externalized.
                    String name = principal.getName();
                    if (name.indexOf('@') < 0) {
                        name += "@default"; // hardcoded (REVISIT!)
                    }
                    byte[] principalName = name.getBytes(StandardCharsets.UTF_8);

                    // encode the principal name as mandated by RFC2743.
                    byte[] encodedName = CSIv2Util.encodeGssExportedName(principalName);

                    // encapsulate the encoded name.
                    Any any = ORB.init().create_any();
                    byte[] encapsulatedEncodedName;
                    GSS_NT_ExportedNameHelper.insert(any, encodedName);
                    try {
                        encapsulatedEncodedName = codec.encode_value(any);
                    } catch (InvalidTypeForEncoding e) {
                        throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
                    }

                    // create identity token.
                    identityToken = new IdentityToken();
                    identityToken.principal_name(encapsulatedEncodedName);
                } else if ((secMech.sas_context_mech.supported_identity_types & ITTAnonymous.value) != 0) {
                    // no run-as or caller identity and the target supports ITTAnonymous: use the anonymous identity.
                    identityToken = new IdentityToken();
                    identityToken.anonymous(true);
                }

                // target might require an additional initial context token with a username/password pair for authentication.
                if ((secMech.as_context_mech.target_requires & EstablishTrustInClient.value) != 0) {
                    encodedAuthenticationToken = this.createInitialContextToken(uri, secMech);
                }
            }
            else if ((secMech.as_context_mech.target_supports & EstablishTrustInClient.value) != 0) {
                // target doesn't require an identity token but supports username/password authentication - try to build
                // an initial context token using the configuration.
                encodedAuthenticationToken = this.createInitialContextToken(uri, secMech);
            }

            if (identityToken != ABSENT_IDENTITY_TOKEN || encodedAuthenticationToken != NO_AUTHENTICATION_TOKEN) {
                // at least one non-null token was created, create EstablishContext message with it.
                EstablishContext message = new EstablishContext(0, // stateless ctx id
                        NO_AUTHORIZATION_TOKEN, identityToken, encodedAuthenticationToken);

                // create SAS context with the EstablishContext message.
                SASContextBody contextBody = new SASContextBody();
                contextBody.establish_msg(message);

                // stuff the SAS context into the outgoing request.
                final Any any = ORB.init().create_any();
                SASContextBodyHelper.insert(any, contextBody);
                ServiceContext sc = new ServiceContext(SAS_CONTEXT_ID, codec.encode_value(any));
                ri.add_request_service_context(sc, true /*replace existing context*/);

            }
        } catch (Exception e) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
        }

    }

    @Override
    public void send_poll(ClientRequestInfo ri) {
    }

    @Override
    public void receive_reply(ClientRequestInfo ri) {
        try {
            ServiceContext sc = ri.get_reply_service_context(SAS_CONTEXT_ID);
            Any msg = codec.decode_value(sc.context_data, SASContextBodyHelper.type());
            SASContextBody contextBody = SASContextBodyHelper.extract(msg);

            // At this point contextBody should contain a CompleteEstablishContext message, which does not require any
            // treatment. ContextError messages should arrive via receive_exception().
            IIOPLogger.ROOT_LOGGER.tracef("receive_reply: got SAS reply, type %d", contextBody.discriminator());

            if (contextBody.discriminator() == MTContextError.value) {
                // should not happen.
                throw IIOPLogger.ROOT_LOGGER.unexpectedContextErrorInSASReply(0, CompletionStatus.COMPLETED_YES);
            }
        } catch (BAD_PARAM e) {
            // no service context with sasContextId: do nothing
        } catch (FormatMismatch | TypeMismatch e) {
            throw IIOPLogger.ROOT_LOGGER.errorParsingSASReply(e, 0, CompletionStatus.COMPLETED_YES);
        }
    }

    @Override
    public void receive_exception(ClientRequestInfo ri) throws ForwardRequest {
        try {
            ServiceContext sc = ri.get_reply_service_context(SAS_CONTEXT_ID);
            Any msg = codec.decode_value(sc.context_data, SASContextBodyHelper.type());
            SASContextBody contextBody = SASContextBodyHelper.extract(msg);

            // At this point contextBody may contain either a CompleteEstablishContext message or a ContextError message.
            // Neither message requires any treatment. We decoded the context body just to check that it contains
            // a well-formed message.
            IIOPLogger.ROOT_LOGGER.tracef("receive_exception: got SAS reply, type %d", contextBody.discriminator());

        } catch (BAD_PARAM e) {
            // no service context with sasContextId: do nothing.
        } catch (FormatMismatch | TypeMismatch e) {
            throw IIOPLogger.ROOT_LOGGER.errorParsingSASReply(e, 0, CompletionStatus.COMPLETED_MAYBE);
        }
    }

    @Override
    public void receive_other(ClientRequestInfo ri) throws ForwardRequest {
    }

    @Override
    public String name() {
        return "ElytronSASClientInterceptor";
    }

    @Override
    public void destroy() {
    }

    /**
     * Get a reference to the current {@link ServiceContainer}.
     *
     * @return a reference to the current {@link ServiceContainer}.
     */
    private ServiceContainer currentServiceContainer() {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
        }
        return CurrentServiceContainer.getServiceContainer();
    }

    /**
     * Build an {@link URI} using the information extracted from the specified {@link ClientRequestInfo}. The format of
     * the URI built by this method is "iiop://hostname:port".
     *
     * @param clientRequestInfo the {@link ClientRequestInfo} used to obtain the target information necessary to build
     *                          the {@link URI}.
     * @return the constructed {@link URI} instance.
     * @throws URISyntaxException if a syntax error is found when building the {@link URI}.
     */
    private URI getURI(final ClientRequestInfo clientRequestInfo) throws URISyntaxException {
        final StringBuilder builder = new StringBuilder("iiop:");
        if (clientRequestInfo instanceof ClientRequestInfoImpl) {
            ClientRequestInfoImpl infoImpl = (ClientRequestInfoImpl) clientRequestInfo;
            CorbaConnection connection = (CorbaConnection) infoImpl.connection();
            if(connection == null) {
                return null;
            }
            ContactInfo info = connection.getContactInfo();
            if (info instanceof SocketOrChannelContactInfoImpl) {
                String hostname = ((SocketOrChannelContactInfoImpl) info).getHost();
                if (hostname != null)
                    builder.append("//").append(hostname);
                int port = ((SocketOrChannelContactInfoImpl) info).getPort();
                if (port > 0)
                    builder.append(":").append(port);
            }
        } else {
            return null;
        }
        return new URI(builder.toString());
    }

    /**
     * Create an encoded {@link InitialContextToken} with an username/password pair obtained from an Elytron client configuration
     * matched by the specified {@link URI}.
     *
     * @param uri the target {@link URI}.
     * @param secMech a reference to the {@link CompoundSecMech} that was found in the {@link ClientRequestInfo}.
     * @return the encoded {@link InitialContextToken}, if a valid username is obtained from the matched configuration;
     *         an empty {@code byte[]} otherwise;
     * @throws Exception if an error occurs while building the encoded {@link InitialContextToken}.
     */
    private byte[] createInitialContextToken(final URI uri, final CompoundSecMech secMech) throws Exception {

        AuthenticationContext authContext = this.authContext == null ? AuthenticationContext.captureCurrent() : this.authContext;
        // obtain the configuration that matches the URI.
        final AuthenticationConfiguration configuration = AUTH_CONFIG_CLIENT.getAuthenticationConfiguration(uri, authContext, -1, null, null);

        // get the callback handler from the configuration and use it to obtain a username/password pair.
        final CallbackHandler handler = AUTH_CONFIG_CLIENT.getCallbackHandler(configuration);
        final NameCallback nameCallback = new NameCallback("Username: ");
        final PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);
        try {
            handler.handle(new Callback[]{nameCallback, passwordCallback});
        } catch (UnsupportedCallbackException e) {
            return NO_AUTHENTICATION_TOKEN;
        }

        // if the name callback contains a valid username we create the initial context token.
        if (nameCallback.getName() != null && !nameCallback.getName().equals(AnonymousPrincipal.getInstance().getName())) {
            byte[] encodedTargetName = secMech.as_context_mech.target_name;
            String name = nameCallback.getName();
            if (name.indexOf('@') < 0) {
                byte[] decodedTargetName = CSIv2Util.decodeGssExportedName(encodedTargetName);
                String targetName = new String(decodedTargetName, StandardCharsets.UTF_8);
                name += "@" + targetName; // "@default"
            }
            byte[] username = name.getBytes(StandardCharsets.UTF_8);
            byte[] password = {};
            if (passwordCallback.getPassword() != null)
                password = new String(passwordCallback.getPassword()).getBytes(StandardCharsets.UTF_8);

            // create the initial context token and ASN.1-encode it, as defined in RFC 2743.
            InitialContextToken authenticationToken = new InitialContextToken(username, password, encodedTargetName);
            return CSIv2Util.encodeInitialContextToken(authenticationToken, codec);
        }
        return NO_AUTHENTICATION_TOKEN;
    }
}
