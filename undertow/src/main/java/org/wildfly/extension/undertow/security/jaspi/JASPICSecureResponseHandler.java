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

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Stuart Douglas
 */
public class JASPICSecureResponseHandler implements HttpHandler {

    private final HttpHandler next;

    public JASPICSecureResponseHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        try {
            next.handleRequest(exchange);
        } finally {
            try {
                JASPICContext context = exchange.getAttachment(JASPICContext.ATTACHMENT_KEY);

                if (!JASPICAuthenticationMechanism.wasAuthExceptionThrown(exchange) && context != null) {
                    ServletRequestContext requestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                    String applicationIdentifier = JASPICAuthenticationMechanism.buildApplicationIdentifier(requestContext);
                    UndertowLogger.ROOT_LOGGER.debugf("secureResponse for layer [%s] and applicationContextIdentifier [%s].", JASPICAuthenticationMechanism.JASPI_HTTP_SERVLET_LAYER, applicationIdentifier);
                    context.getSam().secureResponse(context.getMessageInfo(), new Subject(), JASPICAuthenticationMechanism.JASPI_HTTP_SERVLET_LAYER, applicationIdentifier, context.getCbh());

                    // A SAM can unwrap the HTTP request/response objects - update the servlet request context with the values found in the message info.
                    ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                    servletRequestContext.setServletRequest((HttpServletRequest) context.getMessageInfo().getRequestMessage());
                    servletRequestContext.setServletResponse((HttpServletResponse) context.getMessageInfo().getResponseMessage());
                }
            } catch (Exception e) {
                UndertowLogger.ROOT_LOGGER.errorInvokingSecureResponse(e);
            }
        }
    }

}
