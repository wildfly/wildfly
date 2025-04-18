/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import io.undertow.Handlers;
import io.undertow.predicate.Predicate;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;

/**
 * Like {@link io.undertow.server.HandlerWrapper}, but for filters.
 * @author Paul Ferraro
 */
public interface PredicateHandlerWrapper {

    HttpHandler wrap(Predicate predicate, HttpHandler next);

    static PredicateHandlerWrapper filter(HandlerWrapper wrapper) {
        return new PredicateHandlerWrapper() {
            @Override
            public HttpHandler wrap(Predicate predicate, HttpHandler next) {
                HttpHandler handler = wrapper.wrap(next);
                return (predicate != null) ? Handlers.predicate(predicate, handler, next) : handler;
            }
        };
    }
}
