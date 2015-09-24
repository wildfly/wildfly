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
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.ConduitWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;
import io.undertow.util.ConduitFactory;
import org.jboss.security.auth.callback.JASPICallbackHandler;
import org.jboss.security.auth.callback.JBossCallbackHandler;
import org.jboss.security.auth.message.GenericMessageInfo;
import org.jboss.security.plugins.auth.JASPIServerAuthenticationManager;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.extension.undertow.security.UndertowSecurityAttachments;
import org.xnio.conduits.StreamSinkConduit;

import javax.security.auth.Subject;
import javax.security.auth.message.AuthException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * {@link io.undertow.security.api.AuthenticationMechanism} implementation that enables JASPI-based authentication.
 * </p>
 *
 * @author Stuart Douglas
 * @author Pedro Igor
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JASPICInitialHandler implements HttpHandler {

    private static final String JASPI_HTTP_SERVLET_LAYER = "HttpServlet";

    private static final AttachmentKey<Boolean> ALREADY_WRAPPED = AttachmentKey.create(Boolean.class);

    private final String securityDomain;
    private final HttpHandler next;

    public JASPICInitialHandler(final String securityDomain, HttpHandler next) {
        this.securityDomain = securityDomain;
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        final ServletRequestContext requestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        final JASPIServerAuthenticationManager sam = createJASPIAuthenticationManager();
        final GenericMessageInfo messageInfo = createMessageInfo(exchange, exchange.getSecurityContext());
        final String applicationIdentifier = buildApplicationIdentifier(requestContext);
        final JASPICallbackHandler cbh = new JASPICallbackHandler();

        UndertowLogger.ROOT_LOGGER.debugf("validateRequest for layer [%s] and applicationContextIdentifier [%s]", JASPI_HTTP_SERVLET_LAYER, applicationIdentifier);

        Account cachedAccount = null;
        final JASPICSecurityContext jaspicSecurityContext = (JASPICSecurityContext) exchange.getSecurityContext();
        final AuthenticatedSessionManager sessionManager = exchange.getAttachment(AuthenticatedSessionManager.ATTACHMENT_KEY);

        if (sessionManager != null) {
            AuthenticatedSessionManager.AuthenticatedSession authSession = sessionManager.lookupSession(exchange);
            if(authSession != null) {
                cachedAccount = authSession.getAccount();
                // if there is a cached account we set it in the security context so that the principal is available to
                // SAM modules via request.getUserPrincipal().
                if (cachedAccount != null) {
                    jaspicSecurityContext.setCachedAuthenticatedAccount(cachedAccount);
                }
            }
        }

        boolean isValid = sam.isValid(messageInfo, new Subject(), JASPI_HTTP_SERVLET_LAYER, applicationIdentifier, cbh);

        jaspicSecurityContext.setCachedAuthenticatedAccount(null);

        exchange.putAttachment(JASPICAttachment.ATTACHMENT_KEY, new JASPICAttachment(isValid, requestContext, sam, messageInfo, applicationIdentifier, cbh, cachedAccount));



        // A SAM can wrap the HTTP request/response objects - update the servlet request context with the values found in the message info.
        ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        servletRequestContext.setServletRequest((HttpServletRequest) messageInfo.getRequestMessage());
        servletRequestContext.setServletResponse((HttpServletResponse) messageInfo.getResponseMessage());


        secureResponse(exchange, sam, messageInfo, cbh);
        next.handleRequest(exchange);
    }

    private JASPIServerAuthenticationManager createJASPIAuthenticationManager() {
        return new JASPIServerAuthenticationManager(this.securityDomain, new JBossCallbackHandler());
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

    private boolean wasAuthExceptionThrown(HttpServerExchange exchange) {
        return exchange.getAttachment(UndertowSecurityAttachments.SECURITY_CONTEXT_ATTACHMENT).getData().get(AuthException.class.getName()) != null;
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
        messageInfo.getMap().put(JASPIAuthenticationMechanism.SECURITY_CONTEXT_ATTACHMENT_KEY, securityContext);
        messageInfo.getMap().put(JASPIAuthenticationMechanism.HTTP_SERVER_EXCHANGE_ATTACHMENT_KEY, exchange);

        return messageInfo;
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
