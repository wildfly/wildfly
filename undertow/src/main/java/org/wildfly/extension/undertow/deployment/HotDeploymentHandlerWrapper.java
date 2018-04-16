/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.undertow.deployment;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.wildfly.extension.classchange.DeploymentClassChangeSupport;
import org.wildfly.extension.undertow.UndertowFilter;

import io.undertow.attribute.ExchangeAttribute;
import io.undertow.attribute.ExchangeAttributes;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;

class HotDeploymentHandlerWrapper implements UndertowFilter {

    /**
     * Used for the class change test suite.
     * <p>
     * Undertow normal circumstances we only check for changes every 2 seconds, as developers can't really make changes faster than that, so subsequent
     * requests in a given 2s interval are likely just for resources related to the original request.
     * <p>
     * In our test suite however we make changes much much faster, so we need to check on every request. This avoids the need for long sleeps everywhere in the test suite
     */
    private static final boolean ALWAYS_CHECK_FOR_CLASS_CHANGES = Boolean.getBoolean("org.wildfly.undertow.ALWAYS_CHECK_FOR_CLASS_CHANGES");
    private static final int TWO_SECONDS = 2000;


    private final String remoteKey;
    private final DeploymentClassChangeSupport support;
    private final String path;

    public HotDeploymentHandlerWrapper(String remoteKey, DeploymentClassChangeSupport support, String path) {
        this.remoteKey = remoteKey;
        this.support = support;
        this.path = path;
    }

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        HotReplacementWebsocketHandler hotReplacementWebsocketHandler;
        if (remoteKey != null) {
            hotReplacementWebsocketHandler = new HotReplacementWebsocketHandler(support);
        } else {
            hotReplacementWebsocketHandler = null;
        }

        HttpHandler updateHandler = new HttpHandler() {

            private volatile long nextUpdate;

            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                if (exchange.isInIoThread()) {
                    exchange.dispatch(this);
                }
                if (nextUpdate > System.currentTimeMillis()) {
                    handler.handleRequest(exchange);
                    return;
                }
                synchronized (this) {
                    if (nextUpdate < System.currentTimeMillis()) {
                        try {
                            if (hotReplacementWebsocketHandler != null) {
                                hotReplacementWebsocketHandler.checkForChanges();
                            } else {
                                support.scanForChangedClasses();
                            }
                            if (!ALWAYS_CHECK_FOR_CLASS_CHANGES) {
                                //we update at most once every 2s
                                nextUpdate = System.currentTimeMillis() + TWO_SECONDS;
                            }
                        } catch (Throwable e) {
                            displayErrorPage(exchange, e);
                            return;
                        }
                    }
                }
                handler.handleRequest(exchange);
            }
        };
        if (remoteKey != null) {
            WebSocketProtocolHandshakeHandler websocket = new WebSocketProtocolHandshakeHandler(hotReplacementWebsocketHandler, updateHandler);
            updateHandler = new PredicateHandler(Predicates.equals(new ExchangeAttribute[]{ExchangeAttributes.requestHeader(new HttpString("remote.password")), ExchangeAttributes.constant(remoteKey)}), websocket, updateHandler);
        }
        return new PredicateHandler(Predicates.prefix(path), updateHandler, handler);
    }


    public static void displayErrorPage(HttpServerExchange exchange, final Throwable exception) throws IOException {
        StringBuilder sb = new StringBuilder();
        //todo: make this good
        sb.append("<html><head><title>ERROR</title>");
        sb.append("</head><body><div class=\"header\"><div class=\"error-div\"></div><div class=\"error-text-div\">Hot Class Change Error</div></div>");
        writeLabel(sb, "Stack Trace", "");

        sb.append("<pre>");
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        sb.append(escapeBodyText(stringWriter.toString()));
        sb.append("</pre></body></html>");
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8;");
        exchange.getResponseSender().send(sb.toString());
    }

    private static void writeLabel(StringBuilder sb, String label, String value) {
        sb.append("<div class=\"label\">");
        sb.append(escapeBodyText(label));
        sb.append(":</div><div class=\"value\">");
        sb.append(escapeBodyText(value));
        sb.append("</div><br/>");
    }

    public static String escapeBodyText(final String bodyText) {
        if (bodyText == null) {
            return "null";
        }
        return bodyText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

}
