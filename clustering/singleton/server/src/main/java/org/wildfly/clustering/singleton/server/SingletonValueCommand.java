/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.singleton.server;

import java.util.Optional;

import org.wildfly.clustering.dispatcher.Command;

public class SingletonValueCommand<T> implements Command<Optional<T>, LegacySingletonContext<T>> {
    private static final long serialVersionUID = -2849349352107418635L;

    @Override
    public Optional<T> execute(LegacySingletonContext<T> context) {
        return context.getLocalValue();
    }
}
