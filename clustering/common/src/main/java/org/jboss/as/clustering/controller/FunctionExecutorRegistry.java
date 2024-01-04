/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.msc.service.ServiceName;

/**
 * Registry of {@link FunctionExecutor} objects.
 * @author Paul Ferraro
 * @param <T> the argument type of the function executor
 */
public interface FunctionExecutorRegistry<T> {
    /**
     * Returns the function executor for the service installed using the specified name.
     * @param name a service name
     * @return a function executor
     */
    FunctionExecutor<T> get(ServiceName name);
}
