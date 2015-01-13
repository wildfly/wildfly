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

package org.wildfly.iiop.openjdk.csiv2;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

import org.jboss.security.RunAs;
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
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;

/**
 * <p>
 * This implementation of {@code org.omg.PortableInterceptor.ClientRequestInterceptor} inserts the security attribute
 * service (SAS) context into outgoing IIOP requests and handles the SAS messages received from the target security
 * service in the SAS context of incoming IIOP replies.
 * </p>
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SASClientIdentityInterceptor extends LocalObject implements ClientRequestInterceptor {

    private static final int sasContextId = org.omg.IOP.SecurityAttributeService.value;

    private static final IdentityToken absentIdentityToken;

    static {
        absentIdentityToken = new IdentityToken();
        absentIdentityToken.absent(true);
    }

    private static final AuthorizationElement[] noAuthorizationToken = {};
    private static final byte[] noAuthenticationToken = {};

    private Codec codec;

    /*
     * Username and password of this server, in case it does not use an SSL certificate to authenticate itself when
     * acting as a client.
     */
    private static final String serverUsername = "j2ee"; // hardcoded (REVISIT!)
    private static final String serverPassword = "j2ee"; // hardcoded (REVISIT!)

    /**
     * <p>
     * Creates an instance of {@code SASClientIdentityInterceptor} with the specified codec.
     * </p>
     *
     * @param codec the {@code Codec} to be used to encode and decode the SAS components.
     */
    public SASClientIdentityInterceptor(Codec codec) {
        this.codec = codec;
    }

    @Override
    public void destroy() {
    }

    @Override
    public String name() {
        return "SASClientIdentityInterceptor";
    }

    @Override
    public void send_request(ClientRequestInfo ri) {
        try {
            CompoundSecMech secMech = CSIv2Util.getMatchingSecurityMech(ri, codec,
                    (short) (EstablishTrustInClient.value
                            + IdentityAssertion.value),    /* client supports */
                    (short) 0                               /* client requires */);
            if (secMech == null) {
                return;
            }

            if (IIOPLogger.ROOT_LOGGER.isTraceEnabled()) {
                StringBuilder tmp = new StringBuilder();
                CSIv2Util.toString(secMech, tmp);
                IIOPLogger.ROOT_LOGGER.trace(tmp);
            }

            // these "null tokens" will be changed if needed.
            IdentityToken identityToken = absentIdentityToken;
            byte[] encodedAuthenticationToken = noAuthenticationToken;

            if ((secMech.sas_context_mech.target_supports & IdentityAssertion.value) != 0) {
                // will create identity token.
                RunAs runAs = SecurityActions.peekRunAsIdentity();
                Principal p = (runAs != null) ? runAs : SecurityActions.getPrincipal();

                if (p != null) {
                    // The name scope needs to be externalized.
                    String name = p.getName();
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
            }

            if ((secMech.as_context_mech.target_requires & EstablishTrustInClient.value) != 0) {
                // will create authentication token with the configured pair serverUsername/serverPassword.
                byte[] encodedTargetName = secMech.as_context_mech.target_name;
                String name = serverUsername;
                if (name.indexOf('@') < 0) {
                    byte[] decodedTargetName =
                            CSIv2Util.decodeGssExportedName(encodedTargetName);
                    String targetName = new String(decodedTargetName, StandardCharsets.UTF_8);
                    name += "@" + targetName; // "@default"
                }
                byte[] username = name.getBytes(StandardCharsets.UTF_8);
                // I don't know why there is not a better way to go from char[] -> byte[].
                byte[] password = serverPassword.getBytes(StandardCharsets.UTF_8);

                // create authentication token
                InitialContextToken authenticationToken = new InitialContextToken(username, password, encodedTargetName);
                // ASN.1-encode it, as defined in RFC 2743.
                encodedAuthenticationToken = CSIv2Util.encodeInitialContextToken(authenticationToken, codec);
            }

            if (identityToken != absentIdentityToken || encodedAuthenticationToken != noAuthenticationToken) {
                // at least one non-null token was created, create EstablishContext message with it.
                EstablishContext message = new EstablishContext(0, // stateless ctx id
                        noAuthorizationToken, identityToken, encodedAuthenticationToken);

                // create SAS context with the EstablishContext message.
                SASContextBody contextBody = new SASContextBody();
                contextBody.establish_msg(message);

                // stuff the SAS context into the outgoing request.
                Any any = ORB.init().create_any();
                SASContextBodyHelper.insert(any, contextBody);
                ServiceContext sc = new ServiceContext(sasContextId, codec.encode_value(any));
                ri.add_request_service_context(sc, true /*replace existing context*/);
            }
        } catch (InvalidTypeForEncoding e) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
        }
    }

    @Override
    public void send_poll(ClientRequestInfo ri) {
    }

    @Override
    public void receive_reply(ClientRequestInfo ri) {
        try {
            ServiceContext sc = ri.get_reply_service_context(sasContextId);
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
            // no service context with sasContextId: do nothing.
        } catch (FormatMismatch e) {
            throw IIOPLogger.ROOT_LOGGER.errorParsingSASReply(e, 0,CompletionStatus.COMPLETED_YES);
        } catch (TypeMismatch e) {
            throw IIOPLogger.ROOT_LOGGER.errorParsingSASReply(e, 0,CompletionStatus.COMPLETED_YES);
        }
    }

    @Override
    public void receive_exception(ClientRequestInfo ri) {
        try {
            ServiceContext sc = ri.get_reply_service_context(sasContextId);
            Any msg = codec.decode_value(sc.context_data, SASContextBodyHelper.type());
            SASContextBody contextBody = SASContextBodyHelper.extract(msg);

            // At this point contextBody may contain either a CompleteEstablishContext message or a ContextError message.
            // Neither message requires any treatment. We decoded the contextbody just to check that it contains a
            // well-formed message.
            IIOPLogger.ROOT_LOGGER.tracef("receive_exception: got SAS reply, type %d", contextBody.discriminator());
        } catch (BAD_PARAM e) {
            // no service context with sasContextId: do nothing.
        } catch (FormatMismatch e) {
            throw IIOPLogger.ROOT_LOGGER.errorParsingSASReply(e, 0, CompletionStatus.COMPLETED_MAYBE);
        } catch (TypeMismatch e) {
            throw IIOPLogger.ROOT_LOGGER.errorParsingSASReply(e, 0, CompletionStatus.COMPLETED_MAYBE);
        }
    }

    @Override
    public void receive_other(ClientRequestInfo ri) {
    }
}
