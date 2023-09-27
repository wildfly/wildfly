/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.wildfly.common.function.ExceptionFunction;

/**
 * Encapsulates execution of a function.
 * @author Paul Ferraro
 * @param <T> the type of the function argument
 */
public interface FunctionExecutor<T> {
    /**
     * Executes the given function.
     * @param <R> the return type
     * @param <E> the exception type
     * @param function a function to execute
     * @return the result of the function
     * @throws E if the function fails to execute
     */
    <R, E extends Exception> R execute(ExceptionFunction<T, R, E> function) throws E;
}
