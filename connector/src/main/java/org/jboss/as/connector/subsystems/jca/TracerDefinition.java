/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.Constants.TRACER;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 * @author Stefano Maestri
 */
public class TracerDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_TRACER = PathElement.pathElement(TRACER, TRACER);

    TracerDefinition() {
        super(PATH_TRACER,
                JcaExtension.getResourceDescriptionResolver(PATH_TRACER.getKey()),
                TracerAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (final TracerParameters parameter : TracerParameters.values()) {
            resourceRegistration.registerReadWriteAttribute(parameter.getAttribute(), null, TracerWriteHandler.INSTANCE);
        }

    }

    public enum TracerParameters {
        TRACER_ENABLED(SimpleAttributeDefinitionBuilder.create("enabled", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(ModelNode.FALSE)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .build());

        TracerParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }

}
