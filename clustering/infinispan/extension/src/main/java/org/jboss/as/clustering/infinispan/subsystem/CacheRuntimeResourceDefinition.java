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

import org.infinispan.eviction.ActivationManager;
import org.infinispan.eviction.PassivationManager;
import org.infinispan.interceptors.impl.CacheMgmtInterceptor;
import org.infinispan.interceptors.impl.InvalidationInterceptor;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.MetricHandler;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * @author Paul Ferraro
 */
public class CacheRuntimeResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String name) {
        return PathElement.pathElement("cache", name);
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        if (InfinispanModel.VERSION_10_0_0.requiresTransformation(version)) {
            parent.discardChildResource(WILDCARD_PATH);
        }
    }

    CacheRuntimeResourceDefinition() {
        super(new Parameters(WILDCARD_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH)).setRuntime());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        new MetricHandler<>(new CacheInterceptorMetricExecutor<>(CacheMgmtInterceptor.class), CacheMetric.class).register(registration);
        new MetricHandler<>(new CacheInterceptorMetricExecutor<>(InvalidationInterceptor.class), CacheInvalidationInterceptorMetric.class).register(registration);
        new MetricHandler<>(new CacheComponentMetricExecutor<>(ActivationManager.class), CacheActivationMetric.class).register(registration);
        new MetricHandler<>(new CacheComponentMetricExecutor<>(PassivationManager.class), CachePassivationMetric.class).register(registration);
        new MetricHandler<>(new ClusteredCacheMetricExecutor(), ClusteredCacheMetric.class).register(registration);
        new OperationHandler<>(new CacheInterceptorOperationExecutor<>(CacheMgmtInterceptor.class), CacheOperation.class).register(registration);

        new LockingRuntimeResourceDefinition().register(registration);
        new PartitionHandlingRuntimeResourceDefinition().register(registration);
        new PersistenceRuntimeResourceDefinition().register(registration);
        new TransactionRuntimeResourceDefinition().register(registration);

        return registration;
    }
}
