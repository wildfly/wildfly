/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
