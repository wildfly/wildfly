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

import java.util.Collection;
import java.util.Set;

import org.jboss.msc.service.BatchServiceTarget;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ServiceListener.Inheritance;
import org.jboss.msc.value.Value;

/**
 * @author Paul Ferraro
 */
public class DelegatingServiceTarget implements ServiceTarget {
    private final ServiceTarget target;
    private final ServiceTargetFactory factory;
    private final BatchServiceTargetFactory batchFactory;
    private final ServiceBuilderFactory builderFactory;

    public DelegatingServiceTarget(ServiceTarget target, ServiceTargetFactory factory, BatchServiceTargetFactory batchFactory, ServiceBuilderFactory builderFactory) {
        this.target = target;
        this.builderFactory = builderFactory;
        this.batchFactory = batchFactory;
        this.factory = factory;
    }

    @Override
    public <T> ServiceBuilder<T> addServiceValue(ServiceName name, Value<? extends Service<T>> value) {
        return this.builderFactory.createServiceBuilder(this.target.addServiceValue(name, value));
    }

    @Override
    public <T> ServiceBuilder<T> addService(ServiceName name, Service<T> service) {
        return this.builderFactory.createServiceBuilder(this.target.addService(name, service));
    }

    @Override
    public ServiceTarget addListener(ServiceListener<Object> listener) {
        this.target.addListener(listener);
        return this;
    }

    @Override
    public ServiceTarget addListener(ServiceListener<Object>... listeners) {
        this.target.addListener(listeners);
        return this;
    }

    @Override
    public ServiceTarget addListener(Collection<ServiceListener<Object>> listeners) {
        this.target.addListener(listeners);
        return this;
    }

    @Override
    public ServiceTarget addListener(Inheritance inheritance, ServiceListener<Object> listener) {
        this.target.addListener(inheritance, listener);
        return this;
    }

    @Override
    public ServiceTarget addListener(Inheritance inheritance, ServiceListener<Object>... listeners) {
        this.target.addListener(inheritance, listeners);
        return this;
    }

    @Override
    public ServiceTarget addListener(Inheritance inheritance, Collection<ServiceListener<Object>> listeners) {
        this.target.addListener(inheritance, listeners);
        return this;
    }

    @Override
    public ServiceTarget removeListener(ServiceListener<Object> listener) {
        this.target.removeListener(listener);
        return this;
    }

    @Override
    public Set<ServiceListener<Object>> getListeners() {
        return this.getListeners();
    }

    @Override
    public ServiceTarget addDependency(ServiceName dependency) {
        this.target.addDependency(dependency);
        return this;
    }

    @Override
    public ServiceTarget addDependency(ServiceName... dependencies) {
        this.target.addDependency(dependencies);
        return this;
    }

    @Override
    public ServiceTarget addDependency(Collection<ServiceName> dependencies) {
        this.target.addDependency(dependencies);
        return this;
    }

    @Override
    public ServiceTarget removeDependency(ServiceName dependency) {
        this.target.removeDependency(dependency);
        return this;
    }

    @Override
    public Set<ServiceName> getDependencies() {
        return this.target.getDependencies();
    }

    @Override
    public ServiceTarget subTarget() {
        return this.factory.createServiceTarget(this.target.subTarget());
    }

    @Override
    public BatchServiceTarget batchTarget() {
        return this.batchFactory.createBatchServiceTarget(this.target.batchTarget());
    }
}
