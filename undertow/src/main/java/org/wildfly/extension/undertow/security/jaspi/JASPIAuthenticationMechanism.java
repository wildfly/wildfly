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

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;

import io.undertow.util.StatusCodes;
import org.jboss.security.SecurityConstants;
import org.jboss.security.auth.callback.JBossCallbackHandler;
import org.jboss.security.auth.message.GenericMessageInfo;
import org.jboss.security.identity.plugins.SimpleRole;
import org.jboss.security.identity.plugins.SimpleRoleGroup;
import org.jboss.security.plugins.auth.JASPIServerAuthenticationManager;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.security.AccountImpl;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import org.jboss.security.auth.callback.JASPICallbackHandler;
import org.jboss.security.identity.Role;
import org.jboss.security.identity.RoleGroup;
import org.wildfly.extension.undertow.security.UndertowSecurityAttachments;

import javax.security.auth.message.AuthException;

import javax.security.auth.Subject;

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

    public static final int DEFAULT_ERROR_CODE = StatusCodes.UNAUTHORIZED;

    private final String configuredAuthMethod;
    private final String securityDomain;

    public JASPIAuthenticationMechanism(final String configuredAuthMethod, String securityDomain) {
        this.configuredAuthMethod = configuredAuthMethod;
        this.securityDomain = securityDomain;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(final HttpServerExchange exchange, final SecurityContext sc) {
        JASPICAttachment attachment = exchange.getAttachment(JASPICAttachment.ATTACHMENT_KEY);

        AuthenticationMechanismOutcome outcome;
        Account authenticatedAccount = null;

        Boolean isValid = attachment.getValid();
        attachment.setValid(null);
        GenericMessageInfo messageInfo = attachment.getMessageInfo();
        if(isValid == null) {
            isValid = createJASPIAuthenticationManager().isValid(messageInfo, new Subject(), JASPI_HTTP_SERVLET_LAYER, attachment.getApplicationIdentifier(), new JBossCallbackHandler());
        }

        final ServletRequestContext requestContext = attachment.getRequestContext();
        final JASPIServerAuthenticationManager sam = attachment.getSam();
        final JASPICallbackHandler cbh = attachment.getCbh();

        if (isValid) {
            // The CBH filled in the JBOSS SecurityContext, we need to create an Undertow account based on that
            org.jboss.security.SecurityContext jbossSct = SecurityActions.getSecurityContext();
            authenticatedAccount = createAccount(attachment.getCachedAccount(), jbossSct);
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

            // make sure we don't return status OK if the AuthException was thrown
            if (wasAuthExceptionThrown(exchange) && !statusIndicatesError(exchange)) {
                exchange.setResponseCode(DEFAULT_ERROR_CODE);
            }
        }

        return outcome;

    }

    private JASPIServerAuthenticationManager createJASPIAuthenticationManager() {
        return new JASPIServerAuthenticationManager(this.securityDomain, new JBossCallbackHandler());
    }
    @Override
    public ChallengeResult sendChallenge(final HttpServerExchange exchange, final SecurityContext securityContext) {
        return new ChallengeResult(true);
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

    private boolean wasAuthExceptionThrown(HttpServerExchange exchange) {
        return exchange.getAttachment(UndertowSecurityAttachments.SECURITY_CONTEXT_ATTACHMENT).getData().get(AuthException.class.getName()) != null;
    }

    private boolean statusIndicatesError(HttpServerExchange exchange) {
        return exchange.getResponseCode() != StatusCodes.OK;
    }
}
