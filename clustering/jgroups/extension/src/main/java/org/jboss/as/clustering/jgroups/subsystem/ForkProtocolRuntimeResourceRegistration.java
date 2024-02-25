/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.clustering.controller.RuntimeResourceRegistration;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolMetricsHandler.Attribute;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolMetricsHandler.FieldType;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jgroups.JChannel;
import org.jgroups.stack.Protocol;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Operation handler for registration of fork protocol runtime resources.
 * @author Paul Ferraro
 */
public class ForkProtocolRuntimeResourceRegistration implements RuntimeResourceRegistration {

    private final FunctionExecutorRegistry<JChannel> executors;

    public ForkProtocolRuntimeResourceRegistration(FunctionExecutorRegistry<JChannel> executors) {
        this.executors = executors;
    }

    @Override
    public void register(OperationContext context) throws OperationFailedException {

        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
        String protocolName = context.getCurrentAddressValue();
        String moduleName = ProtocolResourceDefinition.Attribute.MODULE.resolveModelAttribute(context, resource.getModel()).asString();
        Class<? extends Protocol> protocolClass = ChannelRuntimeResourceRegistration.findProtocolClass(context, protocolName, moduleName);

        Map<String, Attribute> attributes = ProtocolMetricsHandler.findProtocolAttributes(protocolClass);

        // If this is a wildcard registration, create an override model registration with which to register protocol-specific metrics
        if (registration.getPathAddress().getLastElement().isWildcard()) {
            OverrideDescriptionProvider provider = new OverrideDescriptionProvider() {
                @Override
                public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
                    Map<String, ModelNode> result = new HashMap<>();
                    for (Attribute attribute : attributes.values()) {
                        ModelNode value = new ModelNode();
                        value.get(ModelDescriptionConstants.DESCRIPTION).set(attribute.getDescription());
                        result.put(attribute.getName(), value);
                    }
                    return result;
                }

                @Override
                public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
                    return Collections.emptyMap();
                }
            };
            registration = registration.registerOverrideModel(protocolName, provider);
        }

        ProtocolMetricsHandler handler = new ProtocolMetricsHandler(this.executors);

        for (Attribute attribute : attributes.values()) {
            String name = attribute.getName();
            FieldType type = FieldType.valueOf(attribute.getType());
            registration.registerMetric(new SimpleAttributeDefinitionBuilder(name, type.getModelType()).setStorageRuntime().build(), handler);
        }
    }

    @Override
    public void unregister(OperationContext context) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
        String protocolName = context.getCurrentAddressValue();
        String moduleName = ProtocolResourceDefinition.Attribute.MODULE.resolveModelAttribute(context, resource.getModel()).asString();
        Class<? extends Protocol> protocolClass = ChannelRuntimeResourceRegistration.findProtocolClass(context, protocolName, moduleName);

        for (String attribute : ProtocolMetricsHandler.findProtocolAttributes(protocolClass).keySet()) {
            registration.unregisterAttribute(attribute);
        }
    }
}
