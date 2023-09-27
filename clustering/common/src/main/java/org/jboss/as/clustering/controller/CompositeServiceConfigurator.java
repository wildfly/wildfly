/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * A {@link ServiceConfigurator} facade for collecting and building a set of {@link ServiceConfigurator} instances.
 * @author Paul Ferraro
 */
public class CompositeServiceConfigurator extends SimpleServiceNameProvider implements ServiceConfigurator, Consumer<ServiceConfigurator> {

    private final List<ServiceConfigurator> configurators = new LinkedList<>();

    public CompositeServiceConfigurator() {
        super(null);
    }

    public CompositeServiceConfigurator(ServiceName name) {
        super(name);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        List<ServiceBuilder<?>> builders = new ArrayList<>(this.configurators.size());
        for (ServiceConfigurator configurator : this.configurators) {
            builders.add(configurator.build(target));
        }
        return new CompositeServiceBuilder<>(builders);
    }

    @Override
    public void accept(ServiceConfigurator configurator) {
        this.configurators.add(configurator);
    }
}
