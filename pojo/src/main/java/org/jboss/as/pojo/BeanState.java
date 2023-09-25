/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo;

/**
 * A MC bean state.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public enum BeanState {
    NOT_INSTALLED,
    DESCRIBED,
    INSTANTIATED,
    CONFIGURED,
    CREATE,
    START,
    INSTALLED;

    /**
     * Get the next state.
     *
     * @return the next state
     */
    public BeanState next() {
        return values()[ordinal() + 1];
    }

    /**
     * Is this instance before state @param.
     *
     * @param state the state to check
     * @return true if before, false otherwise
     */
    public boolean isBefore(BeanState state) {
        return state.ordinal() > ordinal();
    }

    /**
     * Is this instance after state @param.
     *
     * @param state the state to check
     * @return true if after, false otherwise
     */
    public boolean isAfter(BeanState state) {
        return state.ordinal() < ordinal();
    }
}
