/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.http.server.security;

import static org.jboss.as.domain.http.server.Constants.AUTHORIZATION_HEADER;
import static org.jboss.as.domain.http.server.Constants.FORBIDDEN;
import static org.jboss.as.domain.http.server.Constants.HOST;
import static org.jboss.as.domain.http.server.Constants.INTERNAL_SERVER_ERROR;
import static org.jboss.as.domain.http.server.Constants.NEGOTIATE;
import static org.jboss.as.domain.http.server.Constants.UNAUTHORIZED;
import static org.jboss.as.domain.http.server.Constants.WWW_AUTHENTICATE_HEADER;
import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;

import java.io.IOException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.jboss.as.core.security.SubjectUserInfo;
import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.AuthorizingCallbackHandler;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.domain.management.SubjectIdentity;
import org.jboss.com.sun.net.httpserver.Authenticator;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpExchange.AttributeScope;
import org.jboss.util.Base64;

/**
 * A custom {@link Authenticator} to support SPNEGO authentication.
 *
 * This authenticator is also able to wrap another {@link Authenticator} and work with it during the authentication process.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SpnegoAuthenticator extends Authenticator {

    private static final String NEGOTIATE_PREFIX = NEGOTIATE + " ";

    private final SecurityRealm securityRealm;
    private final Authenticator wrapped;

    public SpnegoAuthenticator(final SecurityRealm securityRealm, final Authenticator toWrap) {
        this.securityRealm = securityRealm;
        this.wrapped = toWrap;
    }

    @Override
    public Result authenticate(HttpExchange exchange) {
        // Check if we are already authenticated.
        NegotiationContext context = (NegotiationContext) exchange.getAttribute(NegotiationContext.class.getName(), AttributeScope.CONNECTION);
        if (context != null && context.isEstablished()) {
            ROOT_LOGGER.trace("Using previously authenticated context.");
            return context.createSuccess(exchange);
        }

        // If we have a challenge handle it (We would only receive this if we sent a challenge to the client, had client cert been available no challenge would be sent).
        SubjectIdentity subjectIdentity = null;
        Headers reqHeaders = exchange.getRequestHeaders();
        String authorization = reqHeaders.getFirst(AUTHORIZATION_HEADER);
        if (authorization != null && authorization.startsWith(NEGOTIATE_PREFIX)) {
            ROOT_LOGGER.trace("Processing negotiation response.");
            String base64Header = authorization.substring(NEGOTIATE_PREFIX.length());
            byte[] decoded = Base64.decode(base64Header);

            subjectIdentity = securityRealm.getSubjectIdentity(getHostName(exchange), false);
            if (subjectIdentity != null) {
                try {
                    return Subject.doAs(subjectIdentity.getSubject(), new AcceptAction(exchange, decoded));
                } finally {
                    subjectIdentity.logout();
                }
            }
        }

        // If no challenge give other Authenticator opportunity to handle request.
        Result result = null;
        if (wrapped != null) {
            ROOT_LOGGER.trace("Delegating to wrapped authenticator.");
            result = wrapped.authenticate(exchange);
        } else {
            ROOT_LOGGER.trace("No negotiation response, and no wrapped authenticator.");
        }

        if (result instanceof Success || result instanceof Failure) {
            return result;
        }

        // If not authenticated add our own challenge (if applicable for host name.
        Headers respHeaders = exchange.getResponseHeaders();

        String host = getHostName(exchange);
        subjectIdentity = securityRealm.getSubjectIdentity(host, false);
        if (subjectIdentity != null) {
            subjectIdentity.logout();

            List<String> values = respHeaders.remove(WWW_AUTHENTICATE_HEADER);
            if (values == null) {
                ROOT_LOGGER.trace("No existing WWW-Authenticate header");
                values = new ArrayList<String>(1);
            }
            ROOT_LOGGER.trace("Adding Negotiate challenge");
            values.add(0, NEGOTIATE);
            respHeaders.put(WWW_AUTHENTICATE_HEADER, values);

            return new Retry(UNAUTHORIZED);
        } else {
            ROOT_LOGGER.tracef("No Subject available for host '%s'", host);
        }

        // To reach this point no result successfully created.
        return new Failure(FORBIDDEN);
    }

    private String getHostName(final HttpExchange exchange) {
        String hostName = exchange.getRequestHeaders().getFirst(HOST);
        if (hostName != null) {
            if (hostName.contains(":")) {
                hostName = hostName.substring(0, hostName.indexOf(":"));
            }
            return hostName;
        }

        return null;
    }

    private class AcceptAction implements PrivilegedAction<Result> {

        private final HttpExchange exchange;
        private final byte[] request;

        private AcceptAction(final HttpExchange exchange, final byte[] request) {
            this.exchange = exchange;
            this.request = request;
        }

        @Override
        public Result run() {
            NegotiationContext context = (NegotiationContext) exchange.getAttribute(NegotiationContext.class.getName(), AttributeScope.CONNECTION);
            if (context == null) {
                ROOT_LOGGER.trace("Creating new NegotiationContext");
                context = new NegotiationContext();
                exchange.setAttribute(NegotiationContext.class.getName(), context, AttributeScope.CONNECTION);
            }

            assert !context.isEstablished(); // Checked once above.

            GSSContext gssContext = context.getGssContext();

            try {
                if (gssContext == null) {
                    ROOT_LOGGER.trace("Creating new GSSContext");
                    GSSManager manager = GSSManager.getInstance();
                    gssContext = manager.createContext((GSSCredential) null);

                    context.setGssContext(gssContext);
                }

                byte[] respToken = gssContext.acceptSecContext(request, 0, request.length);

                if (respToken != null) {
                    ROOT_LOGGER.trace("Sending response token");
                    Headers respHeaders = exchange.getResponseHeaders();
                    respHeaders.add(WWW_AUTHENTICATE_HEADER, NEGOTIATE_PREFIX + Base64.encodeBytes(respToken));
                }

                if (context.isEstablished()) {
                    return context.createSuccess(exchange);
                } else {
                    return new Retry(UNAUTHORIZED);
                }
            } catch (GSSException e) {
                return new Failure(FORBIDDEN);
            }
        }

    }

    private class NegotiationContext {

        private GSSContext gssContext;
        private Success success;

        public GSSContext getGssContext() {
            return gssContext;
        }

        public void setGssContext(GSSContext gssContext) {
            this.gssContext = gssContext;
        }

        public boolean isEstablished() {
            return gssContext != null && gssContext.isEstablished();
        }

        private Result createSuccess(HttpExchange exchange) {
            assert isEstablished();
            if (success != null) {
                ROOT_LOGGER.trace("Returning existing Success and identity");
                return success;
            }

            Result response;
            try {
                String name = gssContext.getSrcName().toString();
                SubjectHttpPrincipal shp = new SubjectHttpPrincipal(name, securityRealm.getName());
                response = success = new Success(shp);
                Collection<Principal> principalCol = new HashSet<Principal>();
                principalCol.add(shp);
                AuthorizingCallbackHandler ach = securityRealm.getAuthorizingCallbackHandler(AuthenticationMechanism.KERBEROS);
                AuthorizeCallback ac = new AuthorizeCallback(name, name);
                ach.handle(new Callback[] {ac});
                if (ac.isAuthorized() == false) {
                    // This should not be possible but we have to check.
                    ROOT_LOGGER.debugf("Callback handler denied authorization for '%s'", name);
                    response = new Failure(INTERNAL_SERVER_ERROR);
                }

                SubjectUserInfo userInfo = ach.createSubjectUserInfo(principalCol);

                Subject subject = userInfo.getSubject();
                PrincipalUtil.addInetPrincipal(exchange, subject.getPrincipals());

                shp.setSubject(subject);
            } catch (GSSException e) {
                ROOT_LOGGER.debug("Unable to create SubjectUserInfo", e);
                response = new Failure(INTERNAL_SERVER_ERROR);
            } catch (IOException e) {
                ROOT_LOGGER.debug("Unable to create SubjectUserInfo", e);
                response = new Failure(INTERNAL_SERVER_ERROR);
            } catch (UnsupportedCallbackException e) {
                ROOT_LOGGER.debug("Unable to perform authorization check", e);
                response = new Failure(INTERNAL_SERVER_ERROR);
            }

            return response;
        }
    }

}
