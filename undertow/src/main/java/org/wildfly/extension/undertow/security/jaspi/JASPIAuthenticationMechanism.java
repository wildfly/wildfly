/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.security.jaspi;

import io.undertow.security.api.AuthenticatedSessionManager;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.ConduitWrapper;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;

import io.undertow.util.ConduitFactory;
import org.jboss.security.SecurityConstants;
import org.jboss.security.auth.callback.JBossCallbackHandler;
import org.jboss.security.auth.message.GenericMessageInfo;
import org.jboss.security.identity.plugins.SimpleRole;
import org.jboss.security.identity.plugins.SimpleRoleGroup;
import org.jboss.security.plugins.auth.JASPIServerAuthenticationManager;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.security.AccountImpl;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import org.jboss.security.auth.callback.JASPICallbackHandler;
import org.jboss.security.identity.Role;
import org.jboss.security.identity.RoleGroup;
import org.wildfly.extension.undertow.security.UndertowSecurityAttachments;
import org.xnio.conduits.StreamSinkConduit;

/**
 * <p>
 * {@link AuthenticationMechanism} implementation that enables JASPI-based authentication.
 * </p>
 *
 * @author Pedro Igor
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JASPIAuthenticationMechanism implements AuthenticationMechanism {

    private static final String JASPI_HTTP_SERVLET_LAYER = "HttpServlet";
    private static final String MECHANISM_NAME = "JASPIC";
    private static final String JASPI_AUTH_TYPE = "javax.servlet.http.authType";
    private static final String JASPI_REGISTER_SESSION = "javax.servlet.http.registerSession";

    public static final AttachmentKey<HttpServerExchange> HTTP_SERVER_EXCHANGE_ATTACHMENT_KEY = AttachmentKey.create(HttpServerExchange.class);
    public static final AttachmentKey<SecurityContext> SECURITY_CONTEXT_ATTACHMENT_KEY = AttachmentKey.create(SecurityContext.class);
    private static final AttachmentKey<Boolean> ALREADY_WRAPPED = AttachmentKey.create(Boolean.class);

    private final String securityDomain;
    private final String configuredAuthMethod;

    public JASPIAuthenticationMechanism(final String securityDomain, final String configuredAuthMethod) {
        this.securityDomain = securityDomain;
        this.configuredAuthMethod = configuredAuthMethod;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(final HttpServerExchange exchange, final SecurityContext sc) {
        final ServletRequestContext requestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        final JASPIServerAuthenticationManager sam = createJASPIAuthenticationManager();
        final GenericMessageInfo messageInfo = createMessageInfo(exchange, sc);
        final String applicationIdentifier = buildApplicationIdentifier(requestContext);
        final JASPICallbackHandler cbh = new JASPICallbackHandler();

        UndertowLogger.ROOT_LOGGER.debugf("validateRequest for layer [%s] and applicationContextIdentifier [%s]", JASPI_HTTP_SERVLET_LAYER, applicationIdentifier);

        Account cachedAccount = null;
        final JASPICSecurityContext jaspicSecurityContext = (JASPICSecurityContext) exchange.getSecurityContext();
        final AuthenticatedSessionManager sessionManager = exchange.getAttachment(AuthenticatedSessionManager.ATTACHMENT_KEY);

        if (sessionManager != null) {
            AuthenticatedSessionManager.AuthenticatedSession authSession = sessionManager.lookupSession(exchange);
            cachedAccount = authSession.getAccount();
            // if there is a cached account we set it in the security context so that the principal is available to
            // SAM modules via request.getUserPrincipal().
            if (cachedAccount !=  null) {
                jaspicSecurityContext.setCachedAuthenticatedAccount(cachedAccount);
            }
        }

        AuthenticationMechanismOutcome outcome = AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        Account authenticatedAccount = null;

        boolean isValid = sam.isValid(messageInfo, new Subject(), JASPI_HTTP_SERVLET_LAYER, applicationIdentifier, cbh);
        jaspicSecurityContext.setCachedAuthenticatedAccount(null);

        if (isValid) {
            // The CBH filled in the JBOSS SecurityContext, we need to create an Undertow account based on that
            org.jboss.security.SecurityContext jbossSct = SecurityActions.getSecurityContext();
            authenticatedAccount = createAccount(cachedAccount, jbossSct);
        }

        // authType resolution (check message info first, then check for the configured auth method, then use mech-specific name).
        String authType = (String) messageInfo.getMap().get(JASPI_AUTH_TYPE);
        if (authType == null)
            authType = this.configuredAuthMethod != null ? this.configuredAuthMethod : MECHANISM_NAME;

        if (isValid && authenticatedAccount != null) {
            outcome = AuthenticationMechanismOutcome.AUTHENTICATED;

            Object registerObj = messageInfo.getMap().get(JASPI_REGISTER_SESSION);
            boolean cache = false;
            if(registerObj != null && (registerObj instanceof String)) {
                cache = Boolean.valueOf((String)registerObj);
            }
            sc.authenticationComplete(authenticatedAccount, authType, cache);
        } else if (isValid && authenticatedAccount == null && !isMandatory(requestContext)) {
            outcome = AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        } else {
            outcome = AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
            sc.authenticationFailed("JASPIC authentication failed.", authType);
        }

        // A SAM can wrap the HTTP request/response objects - update the servlet request context with the values found in the message info.
        ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        servletRequestContext.setServletRequest((HttpServletRequest) messageInfo.getRequestMessage());
        servletRequestContext.setServletResponse((HttpServletResponse) messageInfo.getResponseMessage());

        secureResponse(exchange, sam, messageInfo, cbh);

        return outcome;

    }

    @Override
    public ChallengeResult sendChallenge(final HttpServerExchange exchange, final SecurityContext securityContext) {
        return new ChallengeResult(true);
    }

    private boolean wasAuthExceptionThrown(HttpServerExchange exchange) {
        return exchange.getAttachment(UndertowSecurityAttachments.SECURITY_CONTEXT_ATTACHMENT).getData().get(AuthException.class.getName()) != null;
    }

    private JASPIServerAuthenticationManager createJASPIAuthenticationManager() {
        return new JASPIServerAuthenticationManager(this.securityDomain, new JBossCallbackHandler());
    }

    private String buildApplicationIdentifier(final ServletRequestContext attachment) {
        ServletRequest servletRequest = attachment.getServletRequest();
        return servletRequest.getServletContext().getVirtualServerName() + " " + servletRequest.getServletContext().getContextPath();
    }

    private GenericMessageInfo createMessageInfo(final HttpServerExchange exchange, final SecurityContext securityContext) {
        ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);

        GenericMessageInfo messageInfo = new GenericMessageInfo();

        messageInfo.setRequestMessage(servletRequestContext.getServletRequest());
        messageInfo.setResponseMessage(servletRequestContext.getServletResponse());

        messageInfo.getMap().put("javax.security.auth.message.MessagePolicy.isMandatory", isMandatory(servletRequestContext).toString());

        // additional context data, useful to provide access to Undertow resources during the modules processing
        messageInfo.getMap().put(SECURITY_CONTEXT_ATTACHMENT_KEY, securityContext);
        messageInfo.getMap().put(HTTP_SERVER_EXCHANGE_ATTACHMENT_KEY, exchange);

        return messageInfo;
    }

    private Account createAccount(final Account cachedAccount, final org.jboss.security.SecurityContext jbossSct) {
        if (jbossSct == null) {
            throw UndertowLogger.ROOT_LOGGER.nullParamter("org.jboss.security.SecurityContext");
        }

        // null principal: SAM has opted out of the authentication process.
        Principal userPrincipal = jbossSct.getUtil().getUserPrincipal();
        if (userPrincipal == null) {
            return null;
        }

        // SAM handled the same principal found in the cached account: indicates we must use the cached account.
        if (cachedAccount != null && cachedAccount.getPrincipal() == userPrincipal) {
            // populate the security context using the cached account data.
            jbossSct.getUtil().createSubjectInfo(userPrincipal, ((AccountImpl) cachedAccount).getCredential(), null);
            RoleGroup roleGroup = new SimpleRoleGroup(SecurityConstants.ROLES_IDENTIFIER);
            for (String role : cachedAccount.getRoles())
                roleGroup.addRole(new SimpleRole(role));
            jbossSct.getUtil().setRoles(roleGroup);
            return cachedAccount;
        }

        // SAM handled a different principal or there is no cached account: build a new account.
        Set<String> stringRoles = new HashSet<String>();
        RoleGroup roleGroup = jbossSct.getUtil().getRoles();
        if (roleGroup != null) {
            for (Role role : roleGroup.getRoles()) {
                stringRoles.add(role.getRoleName());
            }
        }
        Object credential = jbossSct.getUtil().getCredential();
        Principal original = null;
        if(cachedAccount != null) {
            original = cachedAccount.getPrincipal();
        }
        return new AccountImpl(userPrincipal, stringRoles, credential, original);
    }

    private void secureResponse(final HttpServerExchange exchange, final JASPIServerAuthenticationManager sam, final GenericMessageInfo messageInfo, final JASPICallbackHandler cbh) {
        if(exchange.getAttachment(ALREADY_WRAPPED) != null || exchange.isResponseStarted()) {
            return;
        }
        exchange.putAttachment(ALREADY_WRAPPED, true);
        // we add a response wrapper to properly invoke the secureResponse, after processing the destination
        exchange.addResponseWrapper(new ConduitWrapper<StreamSinkConduit>() {
            @Override
            public StreamSinkConduit wrap(final ConduitFactory<StreamSinkConduit> factory, final HttpServerExchange exchange) {
                ServletRequestContext requestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                String applicationIdentifier = buildApplicationIdentifier(requestContext);

                if (!wasAuthExceptionThrown(exchange)) {
                    UndertowLogger.ROOT_LOGGER.debugf("secureResponse for layer [%s] and applicationContextIdentifier [%s].", JASPI_HTTP_SERVLET_LAYER, applicationIdentifier);
                    sam.secureResponse(messageInfo, new Subject(), JASPI_HTTP_SERVLET_LAYER, applicationIdentifier, cbh);

                    // A SAM can unwrap the HTTP request/response objects - update the servlet request context with the values found in the message info.
                    ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                    servletRequestContext.setServletRequest((HttpServletRequest) messageInfo.getRequestMessage());
                    servletRequestContext.setServletResponse((HttpServletResponse) messageInfo.getResponseMessage());
                }
                return factory.create();
            }
        });
    }

    /**
     * <p>The authentication is mandatory if the servlet has http constraints (eg.: {@link
     * javax.servlet.annotation.HttpConstraint}).</p>
     *
     * @param attachment
     * @return
     */
    private Boolean isMandatory(final ServletRequestContext attachment) {
        return attachment.getExchange().getSecurityContext() != null && attachment.getExchange().getSecurityContext().isAuthenticationRequired();
    }
}
