/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Configures a service that captures the value of a target service.
 * @author Paul Ferraro
 */
public class ServiceValueCaptorServiceConfigurator<T> implements ServiceConfigurator, Service {

    private final ServiceValueCaptor<T> captor;
    private final SupplierDependency<T> dependency;

    public ServiceValueCaptorServiceConfigurator(ServiceValueCaptor<T> captor) {
        this.dependency = new ServiceSupplierDependency<>(captor.getServiceName());
        this.captor = captor;
    }

    @Override
    public ServiceName getServiceName() {
        return this.captor.getServiceName().append("captor");
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        return this.dependency.register(builder)
                .setInstance(this)
                .setInitialMode(ServiceController.Mode.PASSIVE);
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.captor.accept(this.dependency.get());
    }

    @Override
    public void stop(StopContext context) {
        this.captor.accept(null);
    }
}
