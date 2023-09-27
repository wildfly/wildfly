/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache;

import java.util.function.Supplier;

/**
 * Simple {@link IdentifierFactory} that delegates to a supplier.
 * @author Paul Ferraro
 */
public class SimpleIdentifierFactory<I> implements IdentifierFactory<I> {

    private final Supplier<I> factory;

    public SimpleIdentifierFactory(Supplier<I> factory) {
        this.factory = factory;
    }

    @Override
    public I get() {
        return this.factory.get();
    }

    @Override
    public void start() {
        // Do nothing
    }

    @Override
    public void stop() {
        // Do nothing
    }
}
