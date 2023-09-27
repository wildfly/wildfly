/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.passivation;

/**
 * @author Paul Ferraro
 */
public interface Bean extends AutoCloseable {
    /**
     * Returns whether or not this instance has been passivated
     */
    boolean wasPassivated();

    /**
     * Returns whether or not this instance has been activated
     */
    boolean wasActivated();

    default void doNothing() {
    }

    @Override
    void close();
}
