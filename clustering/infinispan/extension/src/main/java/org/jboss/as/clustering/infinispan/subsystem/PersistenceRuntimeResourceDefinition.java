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

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.interceptors.impl.CacheLoaderInterceptor;
import org.jboss.as.clustering.controller.BinaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.FunctionExecutorRegistry;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Paul Ferraro
 */
public class PersistenceRuntimeResourceDefinition extends CacheComponentRuntimeResourceDefinition {

    static final PathElement PATH = pathElement("persistence");

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    PersistenceRuntimeResourceDefinition(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(PATH, StoreResourceDefinition.WILDCARD_PATH);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);
        new MetricHandler<>(new CacheInterceptorMetricExecutor<>(this.executors, CacheLoaderInterceptor.class, BinaryCapabilityNameResolver.GRANDPARENT_PARENT), StoreMetric.class).register(registration);
        return registration;
    }
}
