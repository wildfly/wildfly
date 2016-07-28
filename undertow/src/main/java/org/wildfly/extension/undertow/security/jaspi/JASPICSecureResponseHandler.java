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
                ServletRequestContext requestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                String applicationIdentifier = JASPICAuthenticationMechanism.buildApplicationIdentifier(requestContext);

                if (!JASPICAuthenticationMechanism.wasAuthExceptionThrown(exchange) && context != null) {
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
