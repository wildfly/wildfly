/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Reference that can be associated with an arbitrary thread.
 * @author Paul Ferraro
 */
public interface ThreadContextReference<C> extends ContextReference<C>, Function<Thread, C>, BiConsumer<Thread, C> {

    @Override
    default void accept(C context) {
        this.accept(Thread.currentThread(), context);
    }

    @Override
    default C get() {
        return this.apply(Thread.currentThread());
    }
}
