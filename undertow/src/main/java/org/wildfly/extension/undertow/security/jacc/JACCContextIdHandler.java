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

package org.wildfly.extension.undertow.security.jacc;

import java.security.PrivilegedAction;

import javax.security.jacc.PolicyContext;

import io.undertow.predicate.Predicates;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.servlet.predicate.DispatcherTypePredicate;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * <p>
 * A {@link HttpHandler} that sets the web application JACC contextId in the {@link PolicyContext}. Any previously registered
 * contextId is suspended for the duration of the request and is restored when this handler is done.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JACCContextIdHandler implements HttpHandler {

    private final PrivilegedAction<String> setContextIdAction;
    private final HttpHandler next;

    public JACCContextIdHandler(String contextId, HttpHandler next) {
        this.setContextIdAction = new SetContextIDAction(contextId);
        this.next = next;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // set JACC contextID and forward the request to the next handler.
        String previousContextID = null;
        try {
            previousContextID = setContextID(setContextIdAction);
            next.handleRequest(exchange);
        }
        finally {
            // restore the previous JACC contextID.
            if(WildFlySecurityManager.isChecking()) {
                setContextID(new SetContextIDAction(previousContextID));
            } else {
                PolicyContext.setContextID(previousContextID);
            }
        }
    }

    /**
     * <p>
     * A {@link PrivilegedAction} that sets the contextId in the {@link PolicyContext}.
     * </p>
     */
    private static class SetContextIDAction implements PrivilegedAction<String> {

        private final String contextID;

        SetContextIDAction(String contextID) {
            this.contextID = contextID;
        }

        @Override
        public String run() {
            String currentContextID = PolicyContext.getContextID();
            PolicyContext.setContextID(this.contextID);
            return currentContextID;
        }
    }

    private String setContextID(PrivilegedAction<String> action) {
        if(WildFlySecurityManager.isChecking()) {
            return WildFlySecurityManager.doUnchecked(action);
        }else {
            return action.run();
        }
    }

    public static HandlerWrapper wrapper(final String contextId) {
        return new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler handler) {
                //we only run this on REQUEST or ASYNC invocations
                return new PredicateHandler(Predicates.or(DispatcherTypePredicate.REQUEST, DispatcherTypePredicate.ASYNC), new JACCContextIdHandler(contextId, handler), handler);
            }
        };
    }

}
