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
package org.wildfly.extension.metrics;

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.metrics.internal.WildFlyMetricRegistry;

/**
 * Service to create a metric collector
 */
public class WildFlyMetricRegistryService implements Service {

    public static final ServiceName WILDFLY_METRIC_REGISTRY_SERVICE = ServiceName.JBOSS.append(MetricsExtension.SUBSYSTEM_NAME, "registry");

    private WildFlyMetricRegistry registry;

    static void install(OperationContext context) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(WILDFLY_METRIC_REGISTRY_SERVICE);
        serviceBuilder.setInstance(new WildFlyMetricRegistryService()).install();
    }

    WildFlyMetricRegistryService() {
    }

    @Override
    public void start(StartContext context) {
        this.registry = new WildFlyMetricRegistry();
    }

    @Override
    public void stop(StopContext context) {
        this.registry.close();
    }

    @Override
    public WildFlyMetricRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return registry;
    }
}
