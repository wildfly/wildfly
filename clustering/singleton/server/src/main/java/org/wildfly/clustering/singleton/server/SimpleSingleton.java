/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.Supplier;

import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.SingletonState;
import org.wildfly.common.function.Functions;

/**
 * A simple singleton state provider that delegates to a fixed state or provider of state.
 * @author Paul Ferraro
 */
public class SimpleSingleton implements Singleton {

    private final Supplier<SingletonState> state;

    public SimpleSingleton(SingletonState state) {
        this.state = Functions.constantSupplier(state);
    }

    public SimpleSingleton(Supplier<SingletonState> state) {
        this.state = state;
    }

    @Override
    public SingletonState getSingletonState() {
        return this.state.get();
    }
}
