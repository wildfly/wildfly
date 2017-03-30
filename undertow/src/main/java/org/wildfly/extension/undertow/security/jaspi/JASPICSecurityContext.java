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
package org.wildfly.extension.undertow.security.jaspi;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.config.AuthConfigProvider;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.SecurityContextImpl;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.jboss.security.auth.callback.JASPICallbackHandler;
import org.jboss.security.auth.callback.JBossCallbackHandler;
import org.jboss.security.auth.message.GenericMessageInfo;
import org.jboss.security.plugins.auth.JASPIServerAuthenticationManager;

/**
 * <p>
 * A {@link io.undertow.security.api.SecurityContext} that implements the {@code login} and {@code logout} methods
 * according to the JASPIC 1.1 specification.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class JASPICSecurityContext extends SecurityContextImpl {

    private static final String layer = "HttpServlet";
    private static final CallbackHandler handler = new JASPICallbackHandler();

    private final HttpServerExchange exchange;
    private final JASPIServerAuthenticationManager manager;
    private Account cachedAuthenticatedAccount;

    public JASPICSecurityContext(final HttpServerExchange exchange, final AuthenticationMode mode, final IdentityManager identityManager,
                                 final String securityDomain) {
        super (exchange, mode, identityManager);
        this.exchange = exchange;
        this.manager = new JASPIServerAuthenticationManager(securityDomain, new JBossCallbackHandler());
    }

    /**
     * <p>
     * JASPIC 1.1 specification: if there is an {@code AuthConfigProvider} for the {@code HttpServlet} layer and
     * application context, then @{@code login} must throw a {@code ServletException} which may convey that the
     * exception was caused by an incompatibility between the {@code login} method and the configured authentication
     * mechanism. If there is no such provider, then the container must proceed with the regular {@code login} processing.
     * </p>
     *
     * @param username The username
     * @param password The password
     * @return <code>true</code> if the login succeeded, false otherwise
     * @throws SecurityException if login is called when JASPIC is enabled for application context and layer.
     */
    @Override
    public boolean login(final String username, final String password) {
        // if there is an AuthConfigProvider for the HttpServlet layer and appContext, this method must throw an exception.
        String appContext = this.buildAppContext();
        AuthConfigProvider provider = AuthConfigFactory.getFactory().getConfigProvider(layer, appContext, null);
        if (provider != null) {
            ServletException se = new ServletException("login is not supported by the JASPIC mechanism");
            throw new SecurityException(se);
        }
        return super.login(username, password);
    }

    /**
     * <p>
     * JASPIC 1.1 specification: if there is an {@code AuthConfigProvider} for the {@code HttpServlet} layer and
     * application context, then @{@code logout} must acquire a {@code ServerAuthContext} and call {@code cleanSubject}
     * on the acquired context.
     * </p>
     * <p>
     * The specified {@code Subject} should be non-null and should be the {@code Subject} returning from the most recent
     * call to {@code validateRequest}. In our case, that {@code Subject} is set in the underlying security context, so
     * we must retrieve it from there before calling {@code cleanSubject}.
     * </p>
     * <p>
     * Once {@code cleanSubject} returns, {@code logout} must perform the regular (non-JASPIC) {@code logout} processing.
     * </p>
     */
    @Override
    public void logout() {
        if (!isAuthenticated())
            return;

        // call cleanSubject() if there is an AuthConfigProvider for the HttpServlet layer and appContext.
        String appContext = this.buildAppContext();
        if (AuthConfigFactory.getFactory().getConfigProvider(layer, appContext, null) != null) {
            Subject authenticatedSubject = this.getAuthenticatedSubject();
            MessageInfo messageInfo = this.buildMessageInfo();
            this.manager.cleanSubject(messageInfo, authenticatedSubject, layer, appContext, handler);
        }

        // following the return from cleanSubject(), logout must perform the regular logout processing.
        super.logout();
    }

    /**
     * <p>
     * Overrides the parent method to return the cached authenticated account (that is, the account that was set in the
     * session as a result of a SAM setting the {@code javax.servlet.http.registerSession} property) when the regular
     * account is null. This allows a SAM to retrieve the cached account principal by calling {@code getUserPrincipal()}
     * on {@code HttpServletRequest}.
     * </p>
     *
     * @return the authenticated account (or cached account when it is null).
     */
    @Override
    public Account getAuthenticatedAccount() {
        Account account = super.getAuthenticatedAccount();
        if (account == null)
            account = this.cachedAuthenticatedAccount;
        return account;
    }

    /**
     * <p>
     * Sets the cached authenticated account. This is set by the JASPIC mechanism when it detects an existing account
     * in the session.
     * </p>
     *
     * @param account the cached authenticated account.
     */
    public void setCachedAuthenticatedAccount(final Account account) {
        this.cachedAuthenticatedAccount = account;
    }

    /**
     * <p>
     * Builds the JASPIC application context.
     * </p>
     *
     * @return a {@code String} representing the application context.
     */
    private String buildAppContext() {
        final ServletRequestContext requestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        ServletRequest servletRequest = requestContext.getServletRequest();
        return servletRequest.getServletContext().getVirtualServerName() + " " + servletRequest.getServletContext().getContextPath();
    }

    /**
     * <p>
     * Builds the {@code MessageInfo} instance for the {@code cleanSubject()} call.
     * </p>
     *
     * @return the constructed {@code MessageInfo} object.
     */
    private MessageInfo buildMessageInfo() {
        ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        GenericMessageInfo messageInfo = new GenericMessageInfo();
        messageInfo.setRequestMessage(servletRequestContext.getServletRequest());
        messageInfo.setResponseMessage(servletRequestContext.getServletResponse());
        // when calling cleanSubject, isMandatory must be set to true.
        messageInfo.getMap().put("javax.security.auth.message.MessagePolicy.isMandatory", "true");
        return messageInfo;

    }

    /**
     * <p>
     * Retrieves the authenticated subject from the underlying security context.
     * </p>
     *
     * @return a reference to the authenticated subject.
     */
    private Subject getAuthenticatedSubject() {
        Subject subject = null;
        org.jboss.security.SecurityContext picketBoxContext = SecurityActions.getSecurityContext();
        if (picketBoxContext != null && picketBoxContext.getSubjectInfo() != null)
            subject = picketBoxContext.getSubjectInfo().getAuthenticatedSubject();
        return subject != null ? subject : new Subject();
    }
}