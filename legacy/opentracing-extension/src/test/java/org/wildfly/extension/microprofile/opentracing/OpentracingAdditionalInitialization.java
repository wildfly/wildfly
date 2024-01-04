/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.opentracing;

import static org.wildfly.extension.microprofile.opentracing.SubsystemDefinition.MICROPROFILE_CONFIG_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.opentracing.SubsystemDefinition.WELD_CAPABILITY_NAME;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AdditionalInitialization;

class OpentracingAdditionalInitialization extends AdditionalInitialization.ManagementAdditionalInitialization {

    public static final AdditionalInitialization INSTANCE = new OpentracingAdditionalInitialization();
    private static final long serialVersionUID = 1L;

    @Override
    protected ProcessType getProcessType() {
        return ProcessType.HOST_CONTROLLER;
    }

    @Override
    protected void initializeExtraSubystemsAndModel(ExtensionRegistry extensionRegistry, Resource rootResource, ManagementResourceRegistration rootRegistration, RuntimeCapabilityRegistry capabilityRegistry) {
        super.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration, capabilityRegistry);
        Map<String, Class> capabilities = new HashMap<>();
        capabilities.put(WELD_CAPABILITY_NAME, Void.class);
        capabilities.put(MICROPROFILE_CONFIG_CAPABILITY_NAME, Void.class);
        registerServiceCapabilities(capabilityRegistry, capabilities);
    }
}
