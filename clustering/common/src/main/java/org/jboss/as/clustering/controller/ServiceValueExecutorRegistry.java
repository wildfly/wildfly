/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.msc.service.ServiceName;

/**
 * A registry of captured service value executors.
 * @author Paul Ferraro
 */
public class ServiceValueExecutorRegistry<T> implements ServiceValueRegistry<T>, FunctionExecutorRegistry<T> {

    private final Map<ServiceName, ServiceValueExecutor<T>> executors = new ConcurrentHashMap<>();

    @Override
    public ServiceValueCaptor<T> add(ServiceName name) {
        ServiceValueExecutor<T> executor = new ServiceValueExecutor<>(name);
        ServiceValueExecutor<T> existing = this.executors.putIfAbsent(name, executor);
        return (existing != null) ? existing : executor;
    }

    @Override
    public ServiceValueCaptor<T> remove(ServiceName name) {
        return this.executors.remove(name);
    }

    @Override
    public FunctionExecutor<T> get(ServiceName name) {
        return this.executors.get(name);
    }
}
