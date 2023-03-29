package org.wildfly.extension.microprofile.opentracing;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.capability.registry.RuntimeCapabilityRegistry;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.weld.WeldCapability;

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
        capabilities.put(WELD_CAPABILITY_NAME, WeldCapability.class);
        registerServiceCapabilities(capabilityRegistry, capabilities);
    }
}
