/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.server;

import java.util.Optional;

import org.wildfly.clustering.dispatcher.Command;

/**
 * Command to support {@link org.wildfly.clustering.singleton.SingletonService#getValue()} invocations for legacy MSC singleton services.
 * @author Paul Ferraro
 * @param <T> the service value type
 */
@Deprecated
public enum SingletonValueCommand implements Command<Optional<Object>, LegacySingletonContext<Object>> {
    INSTANCE;

    @SuppressWarnings("unchecked")
    static <T> Command<Optional<T>, LegacySingletonContext<T>> getInstance() {
        return (Command<Optional<T>, LegacySingletonContext<T>>) (Command<?, ?>) INSTANCE;
    }

    @Override
    public Optional<Object> execute(LegacySingletonContext<Object> context) {
        return context.getLocalValue();
    }
}
