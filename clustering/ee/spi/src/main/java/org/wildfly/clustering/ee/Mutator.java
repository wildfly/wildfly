/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee;

/**
 * Indicates that the value represented by this object has changed and needs to be replicated.
 * @author Paul Ferraro
 */
public interface Mutator {
    /**
     * Ensure that this object replicates.
     */
    void mutate();

    /**
     * Trivial {@link Mutator} implementation that does nothing.
     * New cache entries, in particular, don't require mutation.
     */
    Mutator PASSIVE = new Mutator() {
        @Override
        public void mutate() {
            // Do nothing
        }
    };
}
