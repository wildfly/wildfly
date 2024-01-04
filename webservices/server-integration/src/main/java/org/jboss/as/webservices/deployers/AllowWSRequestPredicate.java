/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers;

import org.jboss.wsf.spi.deployment.WSFServlet;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;

public class AllowWSRequestPredicate implements Predicate {

    @Override
    public boolean resolve(HttpServerExchange exchange) {
        ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        return src.getCurrentServlet().getManagedServlet().getServletInfo().getServletClass().equals(WSFServlet.class);
    }

}
