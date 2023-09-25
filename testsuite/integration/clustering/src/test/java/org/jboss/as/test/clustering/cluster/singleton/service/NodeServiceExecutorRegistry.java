/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.singleton.service;

import java.util.function.Supplier;

import org.jboss.as.clustering.controller.FunctionExecutor;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.ServiceValueCaptor;
import org.jboss.as.clustering.controller.ServiceValueExecutorRegistry;
import org.jboss.as.clustering.controller.ServiceValueRegistry;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.group.Node;

/**
 * @author Paul Ferraro
 */
public enum NodeServiceExecutorRegistry implements FunctionExecutorRegistry<Supplier<Node>>, ServiceValueRegistry<Supplier<Node>> {
    INSTANCE;

    private final ServiceValueExecutorRegistry<Supplier<Node>> registry = new ServiceValueExecutorRegistry<>();

    @Override
    public ServiceValueCaptor<Supplier<Node>> add(ServiceName name) {
        return this.registry.add(name);
    }

    @Override
    public ServiceValueCaptor<Supplier<Node>> remove(ServiceName name) {
        return this.registry.remove(name);
    }

    @Override
    public FunctionExecutor<Supplier<Node>> get(ServiceName name) {
        return this.registry.get(name);
    }
}
