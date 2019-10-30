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

import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CSI.CompleteEstablishContext;
import org.omg.CSI.ContextError;
import org.omg.CSI.EstablishContext;
import org.omg.CSI.GSS_NT_ExportedNameHelper;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.IdentityToken;
import org.omg.CSI.MTEstablishContext;
import org.omg.CSI.MTMessageInContext;
import org.omg.CSI.SASContextBody;
import org.omg.CSI.SASContextBodyHelper;
import org.omg.GSSUP.ErrorToken;
import org.omg.GSSUP.ErrorTokenHelper;
import org.omg.GSSUP.GSS_UP_S_G_UNSPECIFIED;
import org.omg.GSSUP.InitialContextToken;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;
import org.wildfly.iiop.openjdk.Constants;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;
import org.wildfly.iiop.openjdk.service.CorbaORBService;

/**
 * <p>
 * This implementation of {@code org.omg.PortableInterceptor.ServerRequestInterceptor} extracts the security attribute
 * service (SAS) context from incoming IIOP and inserts SAS messages into the SAS context of outgoing IIOP replies.
 * </p>
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SASTargetInterceptor extends LocalObject implements ServerRequestInterceptor {

    private static final long serialVersionUID = -4809929027984284871L;

    private static final int sasContextId = org.omg.IOP.SecurityAttributeService.value;

    private static final byte[] empty = new byte[0];

    private static final IdentityToken absent;

    /**
     * scratch field for {@code CompleteEstablishContext} messages
     */
    private static final SASContextBody msgBodyCtxAccepted;

    /**
     * Ready-to-go {@code CompleteEstablishContext} message with context id set to zero
     */
    private static final Any msgCtx0Accepted;

    static {
        // initialize absent.
        absent = new IdentityToken();
        absent.absent(true);

        // initialize msgBodyCtxAccepted (Note that "context stateful" is always set to false. Even if the
        // client wants a stateful context, we negotiate the context down to stateless).
        CompleteEstablishContext ctxAccepted =
                new CompleteEstablishContext(0,  /* context id */
                        false,      /* context stateful */
                        new byte[0] /* no final token */);

        msgBodyCtxAccepted = new SASContextBody();
        msgBodyCtxAccepted.complete_msg(ctxAccepted);

        // initialize msgCtx0Accepted.
        msgCtx0Accepted = createMsgCtxAccepted(0);
    }

    private static Any createMsgCtxAccepted(long contextId) {
        Any any = ORB.init().create_any();
        synchronized (msgBodyCtxAccepted) {
            msgBodyCtxAccepted.complete_msg().client_context_id = contextId;
            SASContextBodyHelper.insert(any, msgBodyCtxAccepted);
        }
        return any;
    }

    private final Codec codec;

    /**
     * scratch field for {@code ContextError} messages
     */
    private final SASContextBody msgBodyCtxError;

    /**
     * ready-to-go {@code ContextError} message with context id set to zero and major status "invalid evidence"
     */
    private final Any msgCtx0Rejected;

    private ThreadLocal<CurrentRequestInfo> threadLocalData = new ThreadLocal<CurrentRequestInfo>() {
        protected synchronized CurrentRequestInfo initialValue() {
            return new CurrentRequestInfo(); // see nested class below
        }
    };

    /**
     * The {@code CurrentRequestInfo} class holds SAS information associated with IIOP request handled by the current thread.
     */
    private static class CurrentRequestInfo {

        boolean sasContextReceived;

        boolean authenticationTokenReceived;

        byte[] incomingUsername;

        byte[] incomingPassword;

        byte[] incomingTargetName;

        IdentityToken incomingIdentity;

        byte[] incomingPrincipalName;

        long contextId;

        Any sasReply;

        boolean sasReplyIsAccept;

        /**
         * <p>
         * Creates an instance of {@code CurrentRequestInfo}.
         * </p>
         */
        CurrentRequestInfo() {
        }
    }

    private Any createMsgCtxError(long contextId, int majorStatus) {
        Any any = ORB.init().create_any();
        synchronized (msgBodyCtxError) {
            msgBodyCtxError.error_msg().client_context_id = contextId;
            msgBodyCtxError.error_msg().major_status = majorStatus;
            SASContextBodyHelper.insert(any, msgBodyCtxError);
        }
        return any;
    }

    /**
     * <p>
     * Creates an instance of {@code SASTargetInterceptor} with the specified codec.
     * </p>
     *
     * @param codec the {@code Codec} used to encode and decode the SAS components.
     */
    public SASTargetInterceptor(Codec codec) {
        this.codec = codec;

        // build encapsulated GSSUP error token for ContextError messages (the error code within the error token is
        // GSS_UP_S_G_UNSPECIFIED, which says nothing about the cause of the error).
        ErrorToken errorToken = new ErrorToken(GSS_UP_S_G_UNSPECIFIED.value);
        Any any = ORB.init().create_any();
        byte[] encapsulatedErrorToken;

        ErrorTokenHelper.insert(any, errorToken);
        try {
            encapsulatedErrorToken = codec.encode_value(any);
        } catch (InvalidTypeForEncoding e) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
        }

        // initialize msgBodyCtxError.
        ContextError ctxError =
                new ContextError(0, /* context id                     */
                        1,          /* major status: invalid evidence */
                        1,          /* minor status (always 1)        */
                        encapsulatedErrorToken);

        msgBodyCtxError = new SASContextBody();
        msgBodyCtxError.error_msg(ctxError);

        // initialize msgCtx0Rejected (major status: invalid evidence).
        msgCtx0Rejected = createMsgCtxError(0, 1);

    }

    /**
     * <p>
     * Indicates whether an SAS context has arrived with the current request or not.
     * </p>
     *
     * @return {@code true} if an SAS context arrived with the current IIOP request; {@code false} otherwise.
     */
    boolean sasContextReceived() {
        CurrentRequestInfo threadLocal = threadLocalData.get();
        return threadLocal.sasContextReceived;
    }

    /**
     * <p>
     * Indicates whether a client authentication token has arrived with the current request or not.
     * </p>
     *
     * @return {@code true} if a client authentication token arrived with the current IIOP request; {@code false}
     *         otherwise.
     */
    boolean authenticationTokenReceived() {
        CurrentRequestInfo threadLocal = threadLocalData.get();
        return threadLocal.authenticationTokenReceived;
    }

    /**
     * <p>
     * Obtains the username that arrived with the current request.
     * </p>
     *
     * @return the username that arrived in the current IIOP request.
     */
    byte[] getIncomingUsername() {
        CurrentRequestInfo threadLocal = threadLocalData.get();
        return threadLocal.incomingUsername;
    }

    /**
     * <p>
     * Obtains the password that arrived in the current request.
     * </p>
     *
     * @return the password that arrived in the current IIOP request.
     */
    byte[] getIncomingPassword() {
        CurrentRequestInfo threadLocal = threadLocalData.get();
        return threadLocal.incomingPassword;
    }

    /**
     * <p>
     * Obtains the target name that arrived in the current request.
     * </p>
     *
     * @return the target name that arrived in the current IIOP request.
     */
    byte[] getIncomingTargetName() {
        CurrentRequestInfo threadLocal = threadLocalData.get();
        return threadLocal.incomingTargetName;
    }

    /**
     * <p>
     * Obtains the {@code org.omg.CSI.IdentityToken} that arrived in the current request.
     * </p>
     *
     * @return the {@code IdentityToken} that arrived in the current IIOP request.
     */
    IdentityToken getIncomingIdentity() {
        CurrentRequestInfo threadLocal = threadLocalData.get();
        return threadLocal.incomingIdentity;
    }

    /**
     * <p>
     * Obtains the principal name that arrived in the current request.
     * </p>
     *
     * @return the principal name that arrived in the current IIOP request.
     */
    byte[] getIncomingPrincipalName() {
        CurrentRequestInfo threadLocal = threadLocalData.get();
        return threadLocal.incomingPrincipalName;
    }

    /**
     * <p>
     * Sets the outgoing SAS reply to <code>ContextError</code>, with major status "invalid evidence".
     * </p>
     */
    void rejectIncomingContext() {
        CurrentRequestInfo threadLocal = threadLocalData.get();

        if (threadLocal.sasContextReceived) {
            threadLocal.sasReply = (threadLocal.contextId == 0) ? msgCtx0Rejected :
                    createMsgCtxError(threadLocal.contextId, 1 /* major status: invalid evidence */);
            threadLocal.sasReplyIsAccept = false;
        }
    }

    @Override
    public String name() {
        return "SASTargetInterceptor";
    }

    @Override
    public void destroy() {
    }

    @Override
    public void receive_request_service_contexts(ServerRequestInfo ri) {
    }

    @Override
    public void receive_request(ServerRequestInfo ri) {
        IIOPLogger.ROOT_LOGGER.tracef("receive_request: %s", ri.operation());

        CurrentRequestInfo threadLocal = threadLocalData.get();

        threadLocal.sasContextReceived = false;
        threadLocal.authenticationTokenReceived = false;
        threadLocal.incomingUsername = empty;
        threadLocal.incomingPassword = empty;
        threadLocal.incomingTargetName = empty;
        threadLocal.incomingIdentity = absent;
        threadLocal.incomingPrincipalName = empty;
        threadLocal.sasReply = null;
        threadLocal.sasReplyIsAccept = false;

        try {

            ServiceContext sc = ri.get_request_service_context(sasContextId);


            Any any = codec.decode_value(sc.context_data, SASContextBodyHelper.type());
            SASContextBody contextBody = SASContextBodyHelper.extract(any);


            if (contextBody != null) {
                if (contextBody.discriminator() == MTMessageInContext.value) {
                    // should not happen, as stateful context requests are always negotiated down to stateless in this implementation.
                    long contextId = contextBody.in_context_msg().client_context_id;
                    threadLocal.sasReply = createMsgCtxError(contextId, 4 /* major status: no context */);
                    throw IIOPLogger.ROOT_LOGGER.missingSASContext();
                } else if (contextBody.discriminator() == MTEstablishContext.value) {
                    EstablishContext message = contextBody.establish_msg();
                    threadLocal.contextId = message.client_context_id;
                    threadLocal.sasContextReceived = true;


                    if (message.client_authentication_token != null && message.client_authentication_token.length > 0) {
                        IIOPLogger.ROOT_LOGGER.trace("Received client authentication token");
                        InitialContextToken authToken = CSIv2Util.decodeInitialContextToken(
                                message.client_authentication_token, codec);
                        if (authToken == null) {
                            threadLocal.sasReply = createMsgCtxError(message.client_context_id, 2 /* major status: invalid mechanism */);
                            throw IIOPLogger.ROOT_LOGGER.errorDecodingInitContextToken();
                        }
                        threadLocal.incomingUsername = authToken.username;
                        threadLocal.incomingPassword = authToken.password;
                        threadLocal.incomingTargetName = CSIv2Util.decodeGssExportedName(authToken.target_name);
                        if (threadLocal.incomingTargetName == null) {
                            threadLocal.sasReply = createMsgCtxError(message.client_context_id, 2 /* major status: invalid mechanism */);
                            throw IIOPLogger.ROOT_LOGGER.errorDecodingTargetInContextToken();
                        }
                        threadLocal.authenticationTokenReceived = true;
                    }
                    if (message.identity_token != null) {
                        IIOPLogger.ROOT_LOGGER.trace("Received identity token");
                        threadLocal.incomingIdentity = message.identity_token;
                        if (message.identity_token.discriminator() == ITTPrincipalName.value) {
                            // Extract the RFC2743-encoded name from CDR encapsulation.
                            Any a = codec.decode_value(message.identity_token.principal_name(),
                                    GSS_NT_ExportedNameHelper.type());
                            byte[] encodedName = GSS_NT_ExportedNameHelper.extract(a);

                            // Decode the principal name.
                            threadLocal.incomingPrincipalName = CSIv2Util.decodeGssExportedName(encodedName);

                            if (threadLocal.incomingPrincipalName == null) {
                                threadLocal.sasReply = createMsgCtxError(message.client_context_id, 2 /* major status: invalid mechanism */);
                                throw IIOPLogger.ROOT_LOGGER.errorDecodingPrincipalName();
                            }
                        }
                    }
                    threadLocal.sasReply = (threadLocal.contextId == 0) ? msgCtx0Accepted :
                            createMsgCtxAccepted(threadLocal.contextId);
                    threadLocal.sasReplyIsAccept = true;
                }
            }
        } catch (BAD_PARAM e) {
            // no service context with sasContextId: do nothing.
        } catch (FormatMismatch e) {
            throw IIOPLogger.ROOT_LOGGER.errorDecodingContextData(this.name(), e);
        } catch (TypeMismatch e) {
            throw IIOPLogger.ROOT_LOGGER.errorDecodingContextData(this.name(), e);
        }
    }

    @Override
    public void send_reply(ServerRequestInfo ri) {
        IIOPLogger.ROOT_LOGGER.tracef("send_reply: %s", ri.operation());
        CurrentRequestInfo threadLocal = (CurrentRequestInfo) threadLocalData.get();

        if (threadLocal.sasReply != null) {
            try {
                ServiceContext sc = new ServiceContext(sasContextId, codec.encode_value(threadLocal.sasReply));
                ri.add_reply_service_context(sc, true);
            } catch (InvalidTypeForEncoding e) {
                throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
            }
        }
    }

    @Override
    public void send_exception(ServerRequestInfo ri) {
        IIOPLogger.ROOT_LOGGER.tracef("send_exception: %s", ri.operation());
        CurrentRequestInfo threadLocal = (CurrentRequestInfo) threadLocalData.get();

        // The check below was added for interoperability  with IONA's ASP 6.0, which throws an
        // ArrayIndexOutOfBoundsException when it receives an IIOP reply carrying both an application exception
        // and a SAS reply CompleteEstablishContext. The flag serves the purpose of refraining fromsending an SAS
        // accept (CompleteEstablishContext) reply together with an exception.
        //
        // The CSIv2 spec does not explicitly disallow an SAS accept in an IIOP exception reply.
        boolean interopIONA = Boolean.valueOf(CorbaORBService.getORBProperty(Constants.INTEROP_IONA));
        if (threadLocal.sasReply != null && !interopIONA) {
            try {
                ServiceContext sc = new ServiceContext(sasContextId, codec.encode_value(threadLocal.sasReply));
                ri.add_reply_service_context(sc, true);
            } catch (InvalidTypeForEncoding e) {
                throw IIOPLogger.ROOT_LOGGER.unexpectedException(e);
            }
        }
    }

    @Override
    public void send_other(ServerRequestInfo ri) {
        // Do nothing. According to the SAS spec, LOCATION_FORWARD reply carries no SAS message.
    }
}
