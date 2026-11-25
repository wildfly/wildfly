/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.SingletonState;

/**
 * Reference to a singleton that returns empty state while not provided.
 * @author Paul Ferraro
 */
public class SingletonReference implements Consumer<Singleton>, Supplier<Singleton> {
    private static final Singleton EMPTY = new SimpleSingleton(new SingletonState() {
        @Override
        public boolean isPrimaryProvider() {
            return false;
        }

        @Override
        public Optional<GroupMember> getPrimaryProvider() {
            return Optional.empty();
        }

        @Override
        public Set<GroupMember> getProviders() {
            return Set.of();
        }
    });

    private volatile Singleton singleton = EMPTY;

    @Override
    public Singleton get() {
        return this.singleton;
    }

    @Override
    public void accept(Singleton singleton) {
        this.singleton = Optional.ofNullable(singleton).orElse(EMPTY);
    }
}
