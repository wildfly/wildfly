/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.msc;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author Paul Ferraro
 */
public class DelegatingServiceContainer extends DelegatingServiceTarget implements ServiceContainer {
    private final ServiceContainer container;
    private final ServiceControllerFactory controllerFactory;

    public DelegatingServiceContainer(ServiceContainer container, ServiceTargetFactory factory, BatchServiceTargetFactory batchFactory, ServiceBuilderFactory builderFactory, ServiceControllerFactory controllerFactory) {
        super(container, factory, batchFactory, builderFactory);
        this.container = container;
        this.controllerFactory = controllerFactory;
    }

    @Override
    public ServiceController<?> getRequiredService(ServiceName serviceName) {
        return this.controllerFactory.createServiceController(this.container.getRequiredService(serviceName));
    }

    @Override
    public ServiceController<?> getService(ServiceName serviceName) {
        return this.controllerFactory.createServiceController(this.container.getService(serviceName));
    }

    @Override
    public List<ServiceName> getServiceNames() {
        return this.container.getServiceNames();
    }

    @Override
    public void shutdown() {
        this.container.shutdown();
    }

    @Override
    public boolean isShutdownComplete() {
        return this.container.isShutdownComplete();
    }

    @Override
    public void addTerminateListener(TerminateListener listener) {
        this.container.addTerminateListener(listener);
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        this.container.awaitTermination();
    }

    @Override
    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        this.container.awaitTermination(timeout, unit);
    }

    @Override
    public void dumpServices() {
        this.container.dumpServices();
    }

    @Override
    public void dumpServices(PrintStream stream) {
        this.container.dumpServices(stream);
    }

    @Override
    public String getName() {
        return this.container.getName();
    }
}
