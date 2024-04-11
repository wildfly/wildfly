/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.micrometer.service;

import static org.jboss.as.controller.OperationContext.AttachmentKey;
import static org.wildfly.extension.micrometer._private.MicrometerExtensionLogger.MICROMETER_LOGGER;

import java.io.IOException;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.micrometer.jmx.JmxMicrometerCollector;
import org.wildfly.extension.micrometer.registry.WildFlyCompositeRegistry;

public class MicrometerRegistryService implements Service {
    public static final AttachmentKey<MicrometerRegistryService> CONFIGURATION_KEY = AttachmentKey.create(MicrometerRegistryService.class);
    private WildFlyCompositeRegistry registry;

    public MicrometerRegistryService() {
        this.registry = new WildFlyCompositeRegistry();
    }

    @Override
    public void start(StartContext context) {
        try {
            new JmxMicrometerCollector(registry).init();
        } catch (IOException e) {
            throw MICROMETER_LOGGER.failedInitializeJMXRegistrar(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        if (registry != null) {
            registry.close();
            registry = null;
        }
    }
}
