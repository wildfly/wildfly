/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Configures a {@link Service} whose value is provided by another {@link Service}.
 * @author Paul Ferraro
 */
public class IdentityServiceConfigurator<T> extends SimpleServiceNameProvider implements ServiceConfigurator {

    private final ServiceName requirementName;
    private final ServiceController.Mode initialMode;

    /**
     * Constructs a new service configurator.
     * @param name the target service name
     * @param targetName the target service
     */
    public IdentityServiceConfigurator(ServiceName name, ServiceName requirementName) {
        this(name, requirementName, ServiceController.Mode.PASSIVE);
    }

    /**
     * Constructs a new service configurator.
     * @param name the target service name
     * @param targetName the target service
     * @param initialMode the initial mode of the configured service.
     */
    public IdentityServiceConfigurator(ServiceName name, ServiceName requirementName, ServiceController.Mode initialMode) {
        super(name);
        assert initialMode != ServiceController.Mode.REMOVE;
        this.requirementName = requirementName;
        this.initialMode = initialMode;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<T> injector = builder.provides(this.getServiceName());
        Supplier<T> requirement = builder.requires(this.requirementName);
        Service service = new FunctionalService<>(injector, Function.identity(), requirement);
        return builder.setInstance(service).setInitialMode(this.initialMode);
    }
}
