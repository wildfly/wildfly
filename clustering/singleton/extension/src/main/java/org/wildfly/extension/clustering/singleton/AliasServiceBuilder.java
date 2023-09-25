/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.singleton;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.IdentityServiceConfigurator;

/**
 * Builds an alias to another service.
 * @author Paul Ferraro
 * @param <T> the type of the target service
 * @deprecated Replaced by {@link IdentityServiceConfigurator}.
 */
@Deprecated
public class AliasServiceBuilder<T> implements Builder<T>, Service<T> {

    private final InjectedValue<T> value = new InjectedValue<>();
    private final ServiceName name;
    private final ServiceName targetName;
    private final Class<T> targetClass;

    /**
     * Constructs a new builder
     * @param name the target service name
     * @param targetName the target service
     * @param targetClass the target service class
     */
    public AliasServiceBuilder(ServiceName name, ServiceName targetName, Class<T> targetClass) {
        this.name = name;
        this.targetName = targetName;
        this.targetClass = targetClass;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public ServiceBuilder<T> build(ServiceTarget target) {
        return target.addService(this.name, this)
                .addDependency(this.targetName, this.targetClass, this.value)
                .setInitialMode(ServiceController.Mode.PASSIVE);
    }

    @Override
    public T getValue() {
        return this.value.getValue();
    }

    @Override
    public void start(StartContext context) {
        // Do nothing
    }

    @Override
    public void stop(StopContext context) {
        // Do nothing
    }
}
