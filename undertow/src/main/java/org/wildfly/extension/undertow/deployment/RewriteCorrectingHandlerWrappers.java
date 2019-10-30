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
package org.wildfly.extension.undertow.deployment;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletPathMatch;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;

/**
 * Handler that works around issues with rewrites() and undertow-handlers.conf.
 * <p>
 * Because the rewrite happens after the initial dispatch this handler detects if
 * the path has been rewritten and updates the servlet target.
 *
 * This is a bit of a hack, it needs a lot more thinking about a clean way to handle
 * this
 */
class RewriteCorrectingHandlerWrappers {

    private static final AttachmentKey<String> OLD_RELATIVE_PATH = AttachmentKey.create(String.class);

    static class PreWrapper implements HandlerWrapper {

        @Override
        public HttpHandler wrap(final HttpHandler handler) {
            return new HttpHandler() {
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    exchange.putAttachment(OLD_RELATIVE_PATH, exchange.getRelativePath());
                    handler.handleRequest(exchange);
                }
            };
        }
    }

    static class PostWrapper implements HandlerWrapper {
        @Override
        public HttpHandler wrap(final HttpHandler handler) {
            return new HttpHandler() {
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    String old = exchange.getAttachment(OLD_RELATIVE_PATH);
                    if(!old.equals(exchange.getRelativePath())) {
                        ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                        ServletPathMatch info = src.getDeployment().getServletPaths().getServletHandlerByPath(exchange.getRelativePath());
                        src.setCurrentServlet(info.getServletChain());
                        src.setServletPathMatch(info);
                    }
                    handler.handleRequest(exchange);
                }
            };
        }
    }
}
