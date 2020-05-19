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
package org.wildfly.extension.microprofile.metrics;

import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.CLIENT_FACTORY_CAPABILITY;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.MANAGEMENT_EXECUTOR;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.WILDFLY_COLLECTOR_SERVICE;
import static org.wildfly.extension.microprofile.metrics._private.MicroProfileMetricsLogger.LOGGER;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.setup.JmxRegistrar;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.jboss.as.controller.LocalModelControllerClient;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service to create a metric collector
 */
public class MetricsCollectorService implements Service<MetricCollector> {

    private final Supplier<ModelControllerClientFactory> modelControllerClientFactory;
    private final Supplier<Executor> managementExecutor;
    private final List<String> exposedSubsystems;
    private final String globalPrefix;

    private MetricCollector metricCollector;
    private JmxRegistrar jmxRegistrar;
    private LocalModelControllerClient modelControllerClient;

    static void install(OperationContext context, List<String> exposedSubsystems, String prefix) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget().addService(WILDFLY_COLLECTOR_SERVICE);
        Supplier<ModelControllerClientFactory> modelControllerClientFactory = serviceBuilder.requires(context.getCapabilityServiceName(CLIENT_FACTORY_CAPABILITY, ModelControllerClientFactory.class));
        Supplier<Executor> managementExecutor = serviceBuilder.requires(context.getCapabilityServiceName(MANAGEMENT_EXECUTOR, Executor.class));
        MetricsCollectorService service = new MetricsCollectorService(modelControllerClientFactory, managementExecutor, exposedSubsystems, prefix);
        serviceBuilder.setInstance(service)
                .install();
    }

    MetricsCollectorService(Supplier<ModelControllerClientFactory> modelControllerClientFactory, Supplier<Executor> managementExecutor, List<String> exposedSubsystems, String globalPrefix) {
        this.modelControllerClientFactory = modelControllerClientFactory;
        this.managementExecutor = managementExecutor;
        this.exposedSubsystems = exposedSubsystems;
        this.globalPrefix = globalPrefix;
    }

    @Override
    public void start(StartContext context) throws StartException {
        jmxRegistrar = new JmxRegistrar();
        try {
            jmxRegistrar.init();
        } catch (IOException e) {
            throw LOGGER.failedInitializeJMXRegistrar(e);
        }

        modelControllerClient = modelControllerClientFactory.get().createClient(managementExecutor.get());

        this.metricCollector = new MetricCollector(modelControllerClient, exposedSubsystems, globalPrefix);
    }

    @Override
    public void stop(StopContext context) {
        for (MetricRegistry registry : new MetricRegistry[]{
                MetricRegistries.get(MetricRegistry.Type.BASE),
                MetricRegistries.get(MetricRegistry.Type.VENDOR)}) {
            for (String name : registry.getNames()) {
                registry.remove(name);
            }
        }

        modelControllerClient.close();

        jmxRegistrar = null;
    }

    @Override
    public MetricCollector getValue() throws IllegalStateException, IllegalArgumentException {
        return metricCollector;
    }
}
