/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.Constants.DISTRIBUTED_WORKMANAGER;
import static org.jboss.as.connector.subsystems.jca.Constants.ELYTRON_ENABLED_NAME;
import static org.jboss.as.connector.subsystems.jca.Constants.ELYTRON_MANAGED_SECURITY;
import static org.jboss.as.connector.subsystems.jca.JcaWorkManagerDefinition.registerSubModels;

import java.util.EnumSet;

import org.jboss.as.connector.metadata.api.common.Security;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.spi.ClusteringDefaultRequirement;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JcaDistributedWorkManagerDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_DISTRIBUTED_WORK_MANAGER = PathElement.pathElement(DISTRIBUTED_WORKMANAGER);
    private final boolean registerRuntimeOnly;

    private JcaDistributedWorkManagerDefinition(final boolean registerRuntimeOnly) {
        super(PATH_DISTRIBUTED_WORK_MANAGER,
                JcaExtension.getResourceDescriptionResolver(PATH_DISTRIBUTED_WORK_MANAGER.getKey()),
                DistributedWorkManagerAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    public static JcaDistributedWorkManagerDefinition createInstance(final boolean registerRuntimeOnly) {
        return new JcaDistributedWorkManagerDefinition(registerRuntimeOnly);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (final AttributeDefinition ad : DWmParameters.getReadOnlyAttributeDefinitions()) {
            resourceRegistration.registerReadOnlyAttribute(ad, ReadResourceNameOperationStepHandler.INSTANCE);
        }

        for (final AttributeDefinition ad : DWmParameters.getRuntimeAttributeDefinitions()) {
            resourceRegistration.registerReadWriteAttribute(ad, null, JcaDistributedWorkManagerWriteHandler.INSTANCE);
        }

    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        registerSubModels(resourceRegistration, registerRuntimeOnly);
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration registration) {
        for (DWmCapabilities capability : EnumSet.allOf(DWmCapabilities.class)) {
            registration.registerCapability(capability.getRuntimeCapability());
        }
    }

    enum DWmParameters {
        NAME(SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING)
                .setAllowExpression(false)
                .setRequired(true)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("name")
                .build()),
        SELECTOR(SimpleAttributeDefinitionBuilder.create("selector", ModelType.STRING)
                .setAllowExpression(true)
                .setRequired(false)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName(Element.SELECTOR.getLocalName())
                .setValidator(new EnumValidator<SelectorValue>(SelectorValue.class, true, true))
                .setDefaultValue(new ModelNode(SelectorValue.PING_TIME.name()))
                .build()),
        POLICY(SimpleAttributeDefinitionBuilder.create("policy", ModelType.STRING)
                .setAllowExpression(true)
                .setRequired(false)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName(Element.POLICY.getLocalName())
                .setValidator(new EnumValidator<PolicyValue>(PolicyValue.class, true, true))
                .setDefaultValue(new ModelNode(PolicyValue.WATERMARK.name()))
                .build()),
        POLICY_OPTIONS(new PropertiesAttributeDefinition.Builder("policy-options", true)
                .setAllowExpression(true)
                .setXmlName(Element.OPTION.getLocalName())
                .build()),
        SELECTOR_OPTIONS(new PropertiesAttributeDefinition.Builder("selector-options", true)
                .setAllowExpression(true)
                .setXmlName(Element.OPTION.getLocalName())
                .build()),
        ELYTRON_ENABLED(new SimpleAttributeDefinitionBuilder(ELYTRON_ENABLED_NAME, ModelType.BOOLEAN, true)
                .setXmlName(Security.Tag.ELYTRON_ENABLED.getLocalName())
                .setAllowExpression(true)
                .setDefaultValue(new ModelNode(ELYTRON_MANAGED_SECURITY))
                .build());


        public static AttributeDefinition[] getAttributeDefinitions() {
            final AttributeDefinition[] returnValue = new AttributeDefinition[DWmParameters.values().length];
            int i = 0;
            for (DWmParameters entry : DWmParameters.values()) {
                returnValue[i] = entry.getAttribute();
                i++;
            }
            return returnValue;
        }

        public static AttributeDefinition[] getRuntimeAttributeDefinitions() {
            return new AttributeDefinition[]{
                    POLICY.getAttribute(),
                    SELECTOR.getAttribute(),
                    POLICY_OPTIONS.getAttribute(),
                    SELECTOR_OPTIONS.getAttribute(),
                    ELYTRON_ENABLED.getAttribute()
            };
        }

        public static AttributeDefinition[] getReadOnlyAttributeDefinitions() {
            return new AttributeDefinition[]{
                    NAME.getAttribute()
            };
        }

        DWmParameters(AttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public AttributeDefinition getAttribute() {
            return attribute;
        }

        private AttributeDefinition attribute;
    }

    enum DWmCapabilities {
        CHANNEL_FACTORY(RuntimeCapability.Builder.of("org.wildfly.connector.workmanager").addRequirements(ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY.getName()).build());

        private final RuntimeCapability<Void> capability;

        DWmCapabilities(RuntimeCapability<Void> capability) {
            this.capability = capability;
        }

        RuntimeCapability<Void> getRuntimeCapability() {
            return this.capability;
        }
    }

    public enum PolicyValue {
        NEVER,
        ALWAYS,
        WATERMARK
    }

    public enum SelectorValue {
        FIRST_AVAILABLE,
        PING_TIME,
        MAX_FREE_THREADS
    }
}
