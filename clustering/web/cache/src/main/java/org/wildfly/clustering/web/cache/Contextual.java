/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache;

import java.util.function.Supplier;

/**
 * Provides access to a local context.
 * @author Paul Ferraro
 */
public interface Contextual<C> {
    /**
     * Returns the context, creating it from the specified factory, if necessary.
     * @param a context factory
     * @return the context.
     */
    C getContext(Supplier<C> factory);
}
