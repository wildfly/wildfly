/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.security.jacc;

import java.security.PrivilegedAction;

import jakarta.security.jacc.PolicyContext;

import io.undertow.predicate.Predicates;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.servlet.api.ThreadSetupHandler;
import io.undertow.servlet.predicate.DispatcherTypePredicate;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * <p>
 * A {@link HttpHandler} that sets the web application Jakarta Authorization contextId in the {@link PolicyContext}. Any previously registered
 * contextId is suspended for the duration of the request and is restored when this handler is done.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class JACCContextIdHandler {
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

    private static String setContextID(PrivilegedAction<String> action) {
        if(WildFlySecurityManager.isChecking()) {
            return WildFlySecurityManager.doUnchecked(action);
        }else {
            return action.run();
        }
    }

    public static HandlerWrapper handlerWrapper(final String contextId) {
        PrivilegedAction<String> setContextIdAction = new SetContextIDAction(contextId);
        return new HandlerWrapper() {
            @Override
            public HttpHandler wrap(final HttpHandler next) {
                //we only run this on REQUEST or ASYNC invocations
                return new PredicateHandler(Predicates.or(DispatcherTypePredicate.REQUEST, DispatcherTypePredicate.ASYNC), new HttpHandler() {

                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        // set Jakarta Authorization contextID and forward the request to the next handler.
                        String previousContextID = null;
                        try {
                            previousContextID = setContextID(setContextIdAction);
                            next.handleRequest(exchange);
                        }
                        finally {
                            // restore the previous Jakarta Authorization contextID.
                            if(WildFlySecurityManager.isChecking()) {
                                setContextID(new SetContextIDAction(previousContextID));
                            } else {
                                PolicyContext.setContextID(previousContextID);
                            }
                        }
                    }

                }, next);
            }
        };
    }

    public static ThreadSetupHandler threadSetupHandler(final String contextId) {
        PrivilegedAction<String> setContextIdAction = new SetContextIDAction(contextId);
        return new ThreadSetupHandler() {

            @Override
            public <T, C> Action<T, C> create(final Action<T, C> action) {
                return new Action<T, C>() {
                    @Override
                    public T call(HttpServerExchange exchange, C context) throws Exception {
                        String previousContextID = null;
                        try {
                            previousContextID = setContextID(setContextIdAction);
                            return action.call(exchange, context);
                        } finally {
                            if (WildFlySecurityManager.isChecking()) {
                                setContextID(new SetContextIDAction(previousContextID));
                            } else {
                                PolicyContext.setContextID(previousContextID);
                            }
                        }
                    }

                };
            }
        };
    }
}