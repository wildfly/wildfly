/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.msc.service.ServiceName;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Executes a given function against the captured value of a service.
 * @author Paul Ferraro
 */
public class ServiceValueExecutor<T> implements ServiceValueCaptor<T>, FunctionExecutor<T> {

    private final ServiceName name;

    private T value = null;

    public ServiceValueExecutor(ServiceName name) {
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public synchronized void accept(T value) {
        this.value = value;
    }

    @Override
    public synchronized <R, E extends Exception> R execute(ExceptionFunction<T, R, E> function) throws E {
        return (this.value != null) ? function.apply(this.value) : null;
    }
}
