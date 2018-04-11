/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.security.digest;

import static io.undertow.UndertowLogger.REQUEST_LOGGER;
import static io.undertow.UndertowMessages.MESSAGES;
import static io.undertow.security.impl.DigestAuthorizationToken.parseHeader;
import static io.undertow.util.Headers.AUTHORIZATION;
import static io.undertow.util.Headers.DIGEST;
import static io.undertow.util.Headers.WWW_AUTHENTICATE;
import static io.undertow.util.StatusCodes.UNAUTHORIZED;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.NonceManager;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.DigestAlgorithm;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.DigestAuthorizationToken;
import io.undertow.security.impl.DigestQop;
import io.undertow.security.impl.SimpleNonceManager;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HexConverter;
import io.undertow.util.StatusCodes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * {@link io.undertow.server.HttpHandler} to handle HTTP Digest authentication, both according to RFC-2617 and draft update to allow additional
 * algorithms to be used.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DigestAuthenticationMechanism implements AuthenticationMechanism {

    private static final String DEFAULT_NAME = "DIGEST";
    private static final String DIGEST_PREFIX = DIGEST + " ";
    private static final int PREFIX_LENGTH = DIGEST_PREFIX.length();
    private static final String OPAQUE_VALUE = "00000000000000000000000000000000";
    private static final byte COLON = ':';

    private final String mechanismName;
    private final IdentityManager identityManager;
    private final boolean validateDigestUrl;

    private static final Set<DigestAuthorizationToken> MANDATORY_REQUEST_TOKENS;

    static {
        Set<DigestAuthorizationToken> mandatoryTokens = new HashSet<>();
        mandatoryTokens.add(DigestAuthorizationToken.USERNAME);
        mandatoryTokens.add(DigestAuthorizationToken.REALM);
        mandatoryTokens.add(DigestAuthorizationToken.NONCE);
        mandatoryTokens.add(DigestAuthorizationToken.DIGEST_URI);
        mandatoryTokens.add(DigestAuthorizationToken.RESPONSE);

        MANDATORY_REQUEST_TOKENS = Collections.unmodifiableSet(mandatoryTokens);
    }

    /**
     * The {@link List} of supported algorithms, this is assumed to be in priority order.
     */
    private final List<DigestAlgorithm> supportedAlgorithms;
    private final List<DigestQop> supportedQops;
    private final String qopString;
    private final String realmName; // TODO - Will offer choice once backing store API/SPI is in.
    private final String domain;
    private final NonceManager nonceManager;

    // Where do session keys fit? Do we just hang onto a session key or keep visiting the user store to check if the password
    // has changed?
    // Maybe even support registration of a session so it can be invalidated?
    // 2013-05-29 - Session keys will be cached, where a cached key is used the IdentityManager is still given the
    //              opportunity to check the Account is still valid.

    public DigestAuthenticationMechanism(final List<DigestAlgorithm> supportedAlgorithms, final List<DigestQop> supportedQops,
            final String realmName, final String domain, final NonceManager nonceManager, boolean validateUri) {
        this(supportedAlgorithms, supportedQops, realmName, domain, nonceManager, DEFAULT_NAME, validateUri);
    }

    public DigestAuthenticationMechanism(final List<DigestAlgorithm> supportedAlgorithms, final List<DigestQop> supportedQops,
            final String realmName, final String domain, final NonceManager nonceManager, final String mechanismName, boolean validateUri) {
        this(supportedAlgorithms, supportedQops, realmName, domain, nonceManager, mechanismName, null, validateUri);
    }

    public DigestAuthenticationMechanism(final List<DigestAlgorithm> supportedAlgorithms, final List<DigestQop> supportedQops,
            final String realmName, final String domain, final NonceManager nonceManager, final String mechanismName, final IdentityManager identityManager, boolean validateUri) {
        this.supportedAlgorithms = supportedAlgorithms;
        this.supportedQops = supportedQops;
        this.realmName = realmName;
        this.domain = domain;
        this.nonceManager = nonceManager;
        this.mechanismName = mechanismName;
        this.identityManager = identityManager;
        this.validateDigestUrl = validateUri;

        if (!supportedQops.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            Iterator<DigestQop> it = supportedQops.iterator();
            sb.append(it.next().getToken());
            while (it.hasNext()) {
                sb.append(",").append(it.next().getToken());
            }
            qopString = sb.toString();
        } else {
            qopString = null;
        }
    }

    public DigestAuthenticationMechanism(final String realmName, final String domain, final String mechanismName, boolean validateUri) {
        this(realmName, domain, mechanismName, null, validateUri);
    }

    public DigestAuthenticationMechanism(final String realmName, final String domain, final String mechanismName, final IdentityManager identityManager, boolean validateUri) {
        this(Collections.singletonList(DigestAlgorithm.MD5), Collections.singletonList(DigestQop.AUTH), realmName, domain, new SimpleNonceManager(), DEFAULT_NAME, identityManager, validateUri);
    }

    @SuppressWarnings("deprecation")
    private IdentityManager getIdentityManager(SecurityContext securityContext) {
        return identityManager != null ? identityManager : securityContext.getIdentityManager();
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(final HttpServerExchange exchange,
                                                       final SecurityContext securityContext) {
        List<String> authHeaders = exchange.getRequestHeaders().get(AUTHORIZATION);
        if (authHeaders != null) {
            for (String current : authHeaders) {
                if (current.startsWith(DIGEST_PREFIX)) {
                    String digestChallenge = current.substring(PREFIX_LENGTH);

                    try {
                        DigestContext context = new DigestContext();
                        Map<DigestAuthorizationToken, String> parsedHeader = parseHeader(digestChallenge);
                        context.setMethod(exchange.getRequestMethod().toString());
                        context.setParsedHeader(parsedHeader);
                        // Some form of Digest authentication is going to occur so get the DigestContext set on the exchange.
                        exchange.putAttachment(DigestContext.ATTACHMENT_KEY, context);

                        return handleDigestHeader(exchange, securityContext);
                    } catch (Exception e) {
                        UndertowLogger.ROOT_LOGGER.unexceptedAuthentificationError(e.getLocalizedMessage(), e);
                    }
                }

                // By this point we had a header we should have been able to verify but for some reason
                // it was not correctly structured.
                return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
        }

        // No suitable header has been found in this request,
        return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
    }

    public AuthenticationMechanismOutcome handleDigestHeader(HttpServerExchange exchange, final SecurityContext securityContext) {
        DigestContext context = exchange.getAttachment(DigestContext.ATTACHMENT_KEY);
        Map<DigestAuthorizationToken, String> parsedHeader = context.getParsedHeader();
        // Step 1 - Verify the set of tokens received to ensure valid values.
        Set<DigestAuthorizationToken> mandatoryTokens = new HashSet<>(MANDATORY_REQUEST_TOKENS);
        if (!supportedAlgorithms.contains(DigestAlgorithm.MD5)) {
            // If we don't support MD5 then the client must choose an algorithm as we can not fall back to MD5.
            mandatoryTokens.add(DigestAuthorizationToken.ALGORITHM);
        }
        if (!supportedQops.isEmpty() && !supportedQops.contains(DigestQop.AUTH)) {
            // If we do not support auth then we are mandating auth-int so force the client to send a QOP
            mandatoryTokens.add(DigestAuthorizationToken.MESSAGE_QOP);
        }

        DigestQop qop = null;
        // This check is early as is increases the list of mandatory tokens.
        if (parsedHeader.containsKey(DigestAuthorizationToken.MESSAGE_QOP)) {
            qop = DigestQop.forName(parsedHeader.get(DigestAuthorizationToken.MESSAGE_QOP));
            if (qop == null || !supportedQops.contains(qop)) {
                // We are also ensuring the client is not trying to force a qop that has been disabled.
                REQUEST_LOGGER.invalidTokenReceived(DigestAuthorizationToken.MESSAGE_QOP.getName(),
                        parsedHeader.get(DigestAuthorizationToken.MESSAGE_QOP));
                // TODO - This actually needs to result in a HTTP 400 Bad Request response and not a new challenge.
                return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
            context.setQop(qop);
            mandatoryTokens.add(DigestAuthorizationToken.CNONCE);
            mandatoryTokens.add(DigestAuthorizationToken.NONCE_COUNT);
        }

        // Check all mandatory tokens are present.
        mandatoryTokens.removeAll(parsedHeader.keySet());
        if (mandatoryTokens.size() > 0) {
            for (DigestAuthorizationToken currentToken : mandatoryTokens) {
                // TODO - Need a better check and possible concatenate the list of tokens - however
                // even having one missing token is not something we should routinely expect.
                REQUEST_LOGGER.missingAuthorizationToken(currentToken.getName());
            }
            // TODO - This actually needs to result in a HTTP 400 Bad Request response and not a new challenge.
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }

        // Perform some validation of the remaining tokens.
        if (!realmName.equals(parsedHeader.get(DigestAuthorizationToken.REALM))) {
            REQUEST_LOGGER.invalidTokenReceived(DigestAuthorizationToken.REALM.getName(),
                    parsedHeader.get(DigestAuthorizationToken.REALM));
            // TODO - This actually needs to result in a HTTP 400 Bad Request response and not a new challenge.
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }

        if (validateDigestUrl) {
            if (parsedHeader.containsKey(DigestAuthorizationToken.DIGEST_URI)) {
                String uri = parsedHeader.get(DigestAuthorizationToken.DIGEST_URI);
                String requestURI = exchange.getRequestURI();
                if (!exchange.getQueryString().isEmpty()) {
                    requestURI = requestURI + "?" + exchange.getQueryString();
                }
                if (!uri.equals(requestURI)) {
                    //it is possible we were given an absolute URI
                    //we reconstruct the URI from the host header to make sure they match up
                    //I am not sure if this is overly strict, however I think it is better
                    //to be safe than sorry
                    requestURI = exchange.getRequestURL();
                    if (!exchange.getQueryString().isEmpty()) {
                        requestURI = requestURI + "?" + exchange.getQueryString();
                    }
                    if (!uri.equals(requestURI)) {
                        //just end the auth process
                        exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                        exchange.endExchange();
                        return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
                    }
                }
            } else {
                return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
        }

        if (parsedHeader.containsKey(DigestAuthorizationToken.OPAQUE)) {
            if (!OPAQUE_VALUE.equals(parsedHeader.get(DigestAuthorizationToken.OPAQUE))) {
                REQUEST_LOGGER.invalidTokenReceived(DigestAuthorizationToken.OPAQUE.getName(),
                        parsedHeader.get(DigestAuthorizationToken.OPAQUE));
                return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
        }

        DigestAlgorithm algorithm;
        if (parsedHeader.containsKey(DigestAuthorizationToken.ALGORITHM)) {
            algorithm = DigestAlgorithm.forName(parsedHeader.get(DigestAuthorizationToken.ALGORITHM));
            if (algorithm == null || !supportedAlgorithms.contains(algorithm)) {
                // We are also ensuring the client is not trying to force an algorithm that has been disabled.
                REQUEST_LOGGER.invalidTokenReceived(DigestAuthorizationToken.ALGORITHM.getName(),
                        parsedHeader.get(DigestAuthorizationToken.ALGORITHM));
                // TODO - This actually needs to result in a HTTP 400 Bad Request response and not a new challenge.
                return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            }
        } else {
            // We know this is safe as the algorithm token was made mandatory
            // if MD5 is not supported.
            algorithm = DigestAlgorithm.MD5;
        }

        try {
            context.setAlgorithm(algorithm);
        } catch (NoSuchAlgorithmException e) {
            /*
             * This should not be possible in a properly configured installation.
             */
            REQUEST_LOGGER.exceptionProcessingRequest(e);
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }

        final String userName = parsedHeader.get(DigestAuthorizationToken.USERNAME);
        final IdentityManager identityManager = getIdentityManager(securityContext);
        final Account account;

        if (algorithm.isSession()) {
            /* This can follow one of the following: -
             *   1 - New session so use DigestCredentialImpl with the IdentityManager to
             *       create a new session key.
             *   2 - Obtain the existing session key from the session store and validate it, just use
             *       IdentityManager to validate account is still active and the current role assignment.
             */
            throw new IllegalStateException("Not yet implemented.");
        } else {
            final DigestCredential credential = new DigestCredentialImpl(context);
            account = identityManager.verify(userName, credential);
        }

        if (account == null) {
            // Authentication has failed, this could either be caused by the user not-existing or it
            // could be caused due to an invalid hash.
            securityContext.authenticationFailed(MESSAGES.authenticationFailed(userName), mechanismName);
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }

        // Step 3 - Verify that the nonce was eligible to be used.
        if (!validateNonceUse(context, parsedHeader, exchange)) {
            // TODO - This is the right place to make use of the decision but the check needs to be much much sooner
            // otherwise a failure server
            // side could leave a packet that could be 're-played' after the failed auth.
            // The username and password verification passed but for some reason we do not like the nonce.
            context.markStale();
            // We do not mark as a failure on the security context as this is not quite a failure, a client with a cached nonce
            // can easily hit this point.
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }

        // We have authenticated the remote user.

        //sendAuthenticationInfoHeader(exchange);
        securityContext.authenticationComplete(account, mechanismName, true);
        return AuthenticationMechanismOutcome.AUTHENTICATED;

        // Step 4 - Set up any QOP related requirements.

        // TODO - Do QOP
    }

    private boolean validateNonceUse(DigestContext context, Map<DigestAuthorizationToken, String> parsedHeader, final HttpServerExchange exchange) {
        String suppliedNonce = parsedHeader.get(DigestAuthorizationToken.NONCE);
        int nonceCount = -1;
        if (parsedHeader.containsKey(DigestAuthorizationToken.NONCE_COUNT)) {
            String nonceCountHex = parsedHeader.get(DigestAuthorizationToken.NONCE_COUNT);

            nonceCount = Integer.parseInt(nonceCountHex, 16);
        }

        context.setNonce(suppliedNonce);
        // TODO - A replay attempt will need an exception.
        return (nonceManager.validateNonce(suppliedNonce, nonceCount, exchange));
    }

    private byte[] createHA2Auth(final DigestContext context, Map<DigestAuthorizationToken, String> parsedHeader) {
        byte[] method = context.getMethod().getBytes(StandardCharsets.UTF_8);
        byte[] digestUri = parsedHeader.get(DigestAuthorizationToken.DIGEST_URI).getBytes(StandardCharsets.UTF_8);

        MessageDigest digest = context.getDigest();
        try {
            digest.update(method);
            digest.update(COLON);
            digest.update(digestUri);

            return HexConverter.convertToHexBytes(digest.digest());
        } finally {
            digest.reset();
        }
    }

    @Override
    public ChallengeResult sendChallenge(final HttpServerExchange exchange, final SecurityContext securityContext) {
        DigestContext context = exchange.getAttachment(DigestContext.ATTACHMENT_KEY);
        boolean stale = context == null ? false : context.isStale();

        StringBuilder rb = new StringBuilder(DIGEST_PREFIX);
        rb.append(Headers.REALM.toString()).append("=\"").append(realmName).append("\",");
        rb.append(Headers.DOMAIN.toString()).append("=\"").append(domain).append("\",");
        // based on security constraints.
        rb.append(Headers.NONCE.toString()).append("=\"").append(nonceManager.nextNonce(null, exchange)).append("\",");
        // Not currently using OPAQUE as it offers no integrity, used for session data leaves it vulnerable to
        // session fixation type issues as well.
        rb.append(Headers.OPAQUE.toString()).append("=\"00000000000000000000000000000000\"");
        if (stale) {
            rb.append(",stale=true");
        }
        if (supportedAlgorithms.size() > 0) {
            // This header will need to be repeated once for each algorithm.
            rb.append(",").append(Headers.ALGORITHM.toString()).append("=%s");
        }
        if (qopString != null) {
            rb.append(",").append(Headers.QOP.toString()).append("=\"").append(qopString).append("\"");
        }

        String theChallenge = rb.toString();
        HeaderMap responseHeader = exchange.getResponseHeaders();
        if (supportedAlgorithms.isEmpty()) {
            responseHeader.add(WWW_AUTHENTICATE, theChallenge);
        } else {
            for (DigestAlgorithm current : supportedAlgorithms) {
                responseHeader.add(WWW_AUTHENTICATE, String.format(theChallenge, current.getToken()));
            }
        }

        return new ChallengeResult(true, UNAUTHORIZED);
    }

    private static class DigestContext {

        static final AttachmentKey<DigestContext> ATTACHMENT_KEY = AttachmentKey.create(DigestContext.class);

        private String method;
        private String nonce;
        private DigestQop qop;
        private MessageDigest digest;
        private boolean stale = false;
        Map<DigestAuthorizationToken, String> parsedHeader;

        String getMethod() {
            return method;
        }

        void setMethod(String method) {
            this.method = method;
        }

        boolean isStale() {
            return stale;
        }

        void markStale() {
            this.stale = true;
        }

        String getNonce() {
            return nonce;
        }

        void setNonce(String nonce) {
            this.nonce = nonce;
        }

        DigestQop getQop() {
            return qop;
        }

        void setQop(DigestQop qop) {
            this.qop = qop;
        }

        void setAlgorithm(DigestAlgorithm algorithm) throws NoSuchAlgorithmException {
            digest = algorithm.getMessageDigest();
        }

        MessageDigest getDigest()  {
            return digest;
        }

        Map<DigestAuthorizationToken, String> getParsedHeader() {
            return parsedHeader;
        }

        void setParsedHeader(Map<DigestAuthorizationToken, String> parsedHeader) {
            this.parsedHeader = parsedHeader;
        }

    }

    private class DigestCredentialImpl implements DigestCredential {

        private final DigestContext context;

        private DigestCredentialImpl(final DigestContext digestContext) {
            this.context = digestContext;
        }

        @Override
        public String getClientDigest() {
            return context.getParsedHeader().get(DigestAuthorizationToken.RESPONSE);
        }

        @Override
        public String getNonce() {
            return context.getParsedHeader().get(DigestAuthorizationToken.NONCE);
        }

        @Override
        public String getNonceCount() {
            return context.getParsedHeader().get(DigestAuthorizationToken.NONCE_COUNT);
        }

        @Override
        public String getClientNonce() {
            return context.getParsedHeader().get(DigestAuthorizationToken.CNONCE);
        }

        @Override
        public String getQop() {
            return context.getQop().getToken();
        }

        @Override
        public String getRealm() {
            return realmName;
        }

        @Override
        public String getHA2() {
            byte[] ha2 = createHA2Auth(context, context.getParsedHeader());
            return new String(ha2, StandardCharsets.UTF_8);
        }

    }

}
