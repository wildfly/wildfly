/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * A {@link CapabilityServiceConfigurator} facade for collecting, configuring, and building; or removing; a set of {@link ServiceConfigurator} instances.
 * @author Paul Ferraro
 */
public class CompositeCapabilityServiceConfigurator extends CompositeServiceConfigurator implements CapabilityServiceConfigurator {
    private final BiConsumer<CapabilityServiceSupport, Consumer<ServiceConfigurator>> consumer;

    public CompositeCapabilityServiceConfigurator(BiConsumer<CapabilityServiceSupport, Consumer<ServiceConfigurator>> consumer) {
        this.consumer = consumer;
    }

    @Override
    public CompositeServiceConfigurator configure(CapabilityServiceSupport support) {
        this.consumer.accept(support, this);
        return this;
    }

    public void remove(OperationContext context) {
        this.consumer.accept(context.getCapabilityServiceSupport(), configurator -> context.removeService(configurator.getServiceName()));
    }
}
