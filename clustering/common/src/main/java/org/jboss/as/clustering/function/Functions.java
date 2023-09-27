/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.function;

import java.util.function.Function;

/**
 * {@link Function} utility methods.
 * @author Paul Ferraro
 */
public class Functions {

    /**
     * Returns a function that always returns its input argument.
     *
     * @param <T> the input type to the function
     * @param <R> the output type of the function
     * @return a function that always returns its input argument
     * @see {@link Function#identity()}
     */
    public static <R, T extends R> Function<T, R> identity() {
        return value -> value;
    }

    private Functions() {
        // Hide
    }
}
