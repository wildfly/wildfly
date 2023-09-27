/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ee;

import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Defines a strategy for invoking a given action.
 * @author Paul Ferraro
 */
public interface Invoker {
    /**
     * Invokes the specified action
     * @param action an action to be invoked
     * @return the result of the action
     * @throws Exception if invocation fails
     */
    <R, E extends Exception> R invoke(ExceptionSupplier<R, E> action) throws E;

    /**
     * Invokes the specified action
     * @param action an action to be invoked
     * @throws Exception if invocation fails
     */
    <E extends Exception> void invoke(ExceptionRunnable<E> action) throws E;
}
