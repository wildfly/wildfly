/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;

/**
 * Abstract acceptor resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public abstract class AbstractTransportDefinition extends PersistentResourceDefinition {

    public static final String CONNECTOR_CAPABILITY_NAME = "org.wildfly.messaging.activemq.connector";
    public static final String ACCEPTOR_CAPABILITY_NAME = "org.wildfly.messaging.activemq.acceptor";
    static final RuntimeCapability<Void> CONNECTOR_CAPABILITY = RuntimeCapability.Builder.of(CONNECTOR_CAPABILITY_NAME, true, ConnectorService.class)
            .setDynamicNameMapper(TransportCapabilityNameMapper.INSTANCE)
            .build();
    private final boolean registerRuntimeOnlyValid;
    private final AttributeDefinition[] attrs;
    protected final boolean isAcceptor;

    private static class TransportCapabilityNameMapper implements Function<PathAddress, String[]> {

        private static final TransportCapabilityNameMapper INSTANCE = new TransportCapabilityNameMapper();

        private TransportCapabilityNameMapper() {
        }

        @Override
        public String[] apply(PathAddress address) {
            String[] result = new String[2];
            PathAddress serverAddress = MessagingServices.getActiveMQServerPathAddress(address);
            if (serverAddress.size() > 0) {
                result[0] = serverAddress.getLastElement().getValue();
            } else {
                result[0] = "external";
            }
            result[1] = address.getLastElement().getValue();
            return result;
        }
    }

    public static class TransportCapabilityReferenceRecorder extends CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder {

        private final boolean external;

        public TransportCapabilityReferenceRecorder(String baseDependentName, String baseRequirementName, boolean external) {
            super(external ? DynamicNameMappers.SIMPLE : DynamicNameMappers.PARENT, baseDependentName, TransportCapabilityNameMapper.INSTANCE, baseRequirementName);
            this.external = external;
        }

        @Override
        public void addCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... attributeValues) {
            processCapabilityRequirement(context, attributeName, false, attributeValues);
        }

        @Override
        public void removeCapabilityRequirements(OperationContext context, Resource resource, String attributeName, String... attributeValues) {
            processCapabilityRequirement(context, attributeName, true, attributeValues);
        }

        private void processCapabilityRequirement(OperationContext context, String attributeName, boolean remove, String... attributeValues) {
            String dependentName = getDependentName(context.getCurrentAddress());
            String requirement = getRequirementName(context.getCurrentAddress());
            for (String att : attributeValues) {
                String requirementName = RuntimeCapability.buildDynamicCapabilityName(requirement, att);
                if (remove) {
                    context.deregisterCapabilityRequirement(requirementName, dependentName, attributeName);
                } else {
                    context.registerAdditionalCapabilityRequirement(requirementName, dependentName, attributeName);
                }
            }
        }

        private String getDependentName(PathAddress address) {
            if (external) {
                return RuntimeCapability.buildDynamicCapabilityName(getBaseDependentName(), DynamicNameMappers.SIMPLE.apply(address));
            }
            return RuntimeCapability.buildDynamicCapabilityName(getBaseDependentName(), DynamicNameMappers.PARENT.apply(address));
        }

        private String getRequirementName(PathAddress address) {
            PathAddress serverAddress = MessagingServices.getActiveMQServerPathAddress(address);
            if (serverAddress.size() > 0) {
                return RuntimeCapability.buildDynamicCapabilityName(getBaseRequirementName(), serverAddress.getLastElement().getValue());
            }
            return getBaseRequirementName();
        }

        @Override
        public String getBaseRequirementName() {
            if (external) {
                return super.getBaseRequirementName() + ".external";
            }
            return super.getBaseRequirementName();
        }

        @Override
        public String[] getRequirementPatternSegments(String dynamicElement, PathAddress registrationAddress) {
            String[] dynamicElements;
            if (!external) {
                dynamicElements = new String[]{"$server"};
            } else {
                dynamicElements = new String[0];
            }
            if (dynamicElement != null && !dynamicElement.isEmpty()) {
                String[] result = new String[dynamicElements.length + 1];
                for (int i = 0; i < dynamicElements.length; i++) {
                    if (dynamicElements[i].charAt(0) == '$') {
                        result[i] = dynamicElements[i].substring(1);
                    } else {
                        result[i] = dynamicElements[i];
                    }
                }
                result[dynamicElements.length] = dynamicElement;
                return result;
            }
            return dynamicElements;
        }
    }

    protected AbstractTransportDefinition(final boolean isAcceptor, final String specificType, final boolean registerRuntimeOnlyValid, AttributeDefinition... attrs) {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(specificType),
                new StandardResourceDescriptionResolver((isAcceptor ? CommonAttributes.ACCEPTOR : CommonAttributes.CONNECTOR),
                        MessagingExtension.RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, false) {
            @Override
            public String getResourceDescription(Locale locale, ResourceBundle bundle) {
                return bundle.getString(specificType);
            }
        })
                .setCapabilities(RuntimeCapability.Builder.of(isAcceptor ? ACCEPTOR_CAPABILITY_NAME : CONNECTOR_CAPABILITY_NAME, true)
                        .setDynamicNameMapper(TransportCapabilityNameMapper.INSTANCE)
                        .build())
                .setAddHandler(isAcceptor ? new ActiveMQReloadRequiredHandlers.AddStepHandler(attrs) : new ConnectorAdd(attrs))
                .setRemoveHandler(new ActiveMQReloadRequiredHandlers.RemoveStepHandler()));
        this.isAcceptor = isAcceptor;
        this.registerRuntimeOnlyValid = registerRuntimeOnlyValid;
        this.attrs = attrs;
    }

    protected AbstractTransportDefinition(final boolean isAcceptor, final String specificType, final boolean registerRuntimeOnlyValid, ModelVersion deprecatedSince, AttributeDefinition... attrs) {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(specificType),
                new StandardResourceDescriptionResolver((isAcceptor ? CommonAttributes.ACCEPTOR : CommonAttributes.CONNECTOR),
                        MessagingExtension.RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, false) {
            @Override
            public String getResourceDescription(Locale locale, ResourceBundle bundle) {
                return bundle.getString(specificType);
            }
        })
                .setAddHandler(isAcceptor ? new ActiveMQReloadRequiredHandlers.AddStepHandler(attrs) : new ConnectorAdd(attrs))
                .setRemoveHandler(new ActiveMQReloadRequiredHandlers.RemoveStepHandler())
                .setDeprecatedSince(deprecatedSince));
        this.isAcceptor = isAcceptor;
        this.registerRuntimeOnlyValid = registerRuntimeOnlyValid;
        this.attrs = attrs;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(attrs);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        OperationStepHandler attributeHandler = new ReloadRequiredWriteAttributeHandler(attrs);
        for (AttributeDefinition attr : attrs) {
            if (!attr.getFlags().contains(AttributeAccess.Flag.STORAGE_RUNTIME)) {
                registry.registerReadWriteAttribute(attr, null, attributeHandler);
            }
        }

        if (isAcceptor) {
            AcceptorControlHandler.INSTANCE.registerAttributes(registry);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registry) {
        if (isAcceptor && registerRuntimeOnlyValid) {
            AcceptorControlHandler.INSTANCE.registerOperations(registry, getResourceDescriptionResolver());
        }

        super.registerOperations(registry);
    }
}
