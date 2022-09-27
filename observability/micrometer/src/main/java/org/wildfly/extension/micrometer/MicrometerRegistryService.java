/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.micrometer;

import static org.wildfly.extension.micrometer.MicrometerSubsystemDefinition.MICROMETER_REGISTRY_RUNTIME_CAPABILITY;

import java.io.IOException;
import java.util.function.Consumer;

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.micrometer.jmx.JmxMicrometerCollector;
import org.wildfly.extension.micrometer.metrics.WildFlyRegistry;

class MicrometerRegistryService implements Service {
    private final Consumer<WildFlyRegistry> registriesConsumer;
    private WildFlyRegistry registry;

    static void install(OperationContext context, boolean securityEnabled) {
        ServiceBuilder<?> serviceBuilder = context.getServiceTarget()
                .addService(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName());

        serviceBuilder.setInstance(new MicrometerRegistryService(
                serviceBuilder.provides(MICROMETER_REGISTRY_RUNTIME_CAPABILITY.getCapabilityServiceName())
        )).install();
    }

    private MicrometerRegistryService(Consumer<WildFlyRegistry> registriesConsumer) {
        this.registriesConsumer = registriesConsumer;
    }

    @Override
    public void start(StartContext context) {
        registry = new WildFlyRegistry();

        try {
            // register metrics from JMX MBeans for base metrics
            new JmxMicrometerCollector(registry).init();
        } catch (IOException e) {
            throw MicrometerExtensionLogger.MICROMETER_LOGGER.failedInitializeJMXRegistrar(e);
        }

        registriesConsumer.accept(registry);
    }

    @Override
    public void stop(StopContext context) {
        // Clear registries?
    }
}
