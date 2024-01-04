/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.DeprecationData;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.TimeUnitValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
@Deprecated
public abstract class LegacyPassivationStoreResourceDefinition extends SimpleResourceDefinition {

    static final ModelVersion DEPRECATED_VERSION = ModelVersion.create(2, 0, 0);

    @Deprecated
    static final SimpleAttributeDefinition IDLE_TIMEOUT = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.IDLE_TIMEOUT, ModelType.LONG, true)
            .setXmlName(EJB3SubsystemXMLAttribute.IDLE_TIMEOUT.getLocalName())
            .setDefaultValue(new ModelNode().set(300))
            .setAllowExpression(true)
            .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .build()
    ;
    @Deprecated
    static final SimpleAttributeDefinition IDLE_TIMEOUT_UNIT = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.IDLE_TIMEOUT_UNIT, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.IDLE_TIMEOUT_UNIT.getLocalName())
            .setValidator(new TimeUnitValidator(true,true))
            .setDefaultValue(new ModelNode().set(TimeUnit.SECONDS.name()))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setAllowExpression(true)
            .setDeprecated(DEPRECATED_VERSION)
            .build()
    ;
    @Deprecated
    static final SimpleAttributeDefinitionBuilder MAX_SIZE_BUILDER = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.MAX_SIZE, ModelType.INT, true)
            .setXmlName(EJB3SubsystemXMLAttribute.MAX_SIZE.getLocalName())
            .setDefaultValue(new ModelNode().set(100000))
            .setAllowExpression(true)
            .setValidator(new LongRangeValidator(1, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
    ;

    private final AttributeDefinition[] attributes;

    LegacyPassivationStoreResourceDefinition(String element, OperationStepHandler addHandler, OperationStepHandler removeHandler, OperationEntry.Flag addRestartLevel, OperationEntry.Flag removeRestartLevel, AttributeDefinition... attributes) {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(element), EJB3Extension.getResourceDescriptionResolver(element))
                .setAddHandler(addHandler)
                .setRemoveHandler(removeHandler)
                .setAddRestartLevel(addRestartLevel)
                .setRemoveRestartLevel(removeRestartLevel)
                .setDeprecationData(new DeprecationData(DEPRECATED_VERSION)));
        this.attributes = attributes;
    }

    LegacyPassivationStoreResourceDefinition(String element, OperationStepHandler addHandler, OperationStepHandler removeHandler, OperationEntry.Flag addRestartLevel, OperationEntry.Flag removeRestartLevel, RuntimeCapability capability, AttributeDefinition... attributes) {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(element), EJB3Extension.getResourceDescriptionResolver(element))
                .setAddHandler(addHandler)
                .setRemoveHandler(removeHandler)
                .setAddRestartLevel(addRestartLevel)
                .setRemoveRestartLevel(removeRestartLevel)
                .setDeprecationData(new DeprecationData(DEPRECATED_VERSION))
                .setCapabilities(capability));
        this.attributes = attributes;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(this.attributes);
        for (AttributeDefinition definition: this.attributes) {
            resourceRegistration.registerReadWriteAttribute(definition, null, writeHandler);
        }
    }

}
