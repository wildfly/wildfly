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

import java.util.Set;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.ServiceListener.Inheritance;

/**
 * @author Paul Ferraro
 */
public class DelegatingServiceController<T> implements ServiceController<T> {
    private final ServiceController<T> controller;
    private final ServiceControllerFactory factory;
    private final ServiceContainerFactory containerFactory;

    public DelegatingServiceController(ServiceController<T> controller, ServiceControllerFactory factory, ServiceContainerFactory containerFactory) {
        this.controller = controller;
        this.factory = factory;
        this.containerFactory = containerFactory;
    }

    @Override
    public ServiceController<?> getParent() {
        return this.factory.createServiceController(this.controller.getParent());
    }

    @Override
    public ServiceContainer getServiceContainer() {
        return this.containerFactory.createServiceContainer(this.controller.getServiceContainer());
    }

    @Override
    public ServiceController.Mode getMode() {
        return this.controller.getMode();
    }

    @Override
    public boolean compareAndSetMode(ServiceController.Mode expected, ServiceController.Mode newMode) {
        return this.controller.compareAndSetMode(expected, newMode);
    }

    @Override
    public void setMode(ServiceController.Mode mode) {
        this.controller.setMode(mode);
    }

    @Override
    public ServiceController.State getState() {
        return this.controller.getState();
    }

    @Override
    public ServiceController.Substate getSubstate() {
        return this.controller.getSubstate();
    }

    @Override
    public T getValue() {
        return this.controller.getValue();
    }

    @Override
    public Service<T> getService() {
        return this.controller.getService();
    }

    @Override
    public ServiceName getName() {
        return this.controller.getName();
    }

    @Override
    public ServiceName[] getAliases() {
        return this.controller.getAliases();
    }

    @Override
    public void addListener(ServiceListener<? super T> serviceListener) {
        this.controller.addListener(serviceListener);
    }

    @Override
    public void addListener(Inheritance inheritance, ServiceListener<Object> serviceListener) {
        this.controller.addListener(inheritance, serviceListener);
    }

    @Override
    public void removeListener(ServiceListener<? super T> serviceListener) {
        this.controller.removeListener(serviceListener);
    }

    @Override
    public StartException getStartException() {
        return this.controller.getStartException();
    }

    @Override
    public void retry() {
        this.controller.retry();
    }

    @Override
    public Set<ServiceName> getImmediateUnavailableDependencies() {
        return this.controller.getImmediateUnavailableDependencies();
    }
}
