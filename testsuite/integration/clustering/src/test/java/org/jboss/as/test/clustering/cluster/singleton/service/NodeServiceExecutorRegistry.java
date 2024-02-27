/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.service;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.group.Node;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.service.capture.FunctionExecutorRegistry;
import org.wildfly.service.capture.ServiceValueExecutorRegistry;
import org.wildfly.service.capture.ServiceValueRegistry;

/**
 * @author Paul Ferraro
 */
public enum NodeServiceExecutorRegistry implements FunctionExecutorRegistry<ServiceName, Supplier<Node>>, ServiceValueRegistry<Supplier<Node>> {
    INSTANCE;

    private final ServiceValueExecutorRegistry<Supplier<Node>> registry = ServiceValueExecutorRegistry.newInstance();

    @Override
    public Consumer<Supplier<Node>> add(ServiceName name) {
        return this.registry.add(name);
    }

    @Override
    public void remove(ServiceName name) {
        this.registry.remove(name);
    }

    @Override
    public FunctionExecutor<Supplier<Node>> getExecutor(ServiceName name) {
        return this.registry.getExecutor(name);
    }
}
