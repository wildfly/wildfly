/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.Constants.BOOTSTRAP_CONTEXT;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JcaBootstrapContextDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_BOOTSTRAP_CONTEXT = PathElement.pathElement(BOOTSTRAP_CONTEXT);

    JcaBootstrapContextDefinition() {
        super(PATH_BOOTSTRAP_CONTEXT,
                JcaExtension.getResourceDescriptionResolver(PATH_BOOTSTRAP_CONTEXT.getKey()),
                BootstrapContextAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (final BootstrapCtxParameters parameter : BootstrapCtxParameters.values()) {
            AttributeDefinition ad = parameter.getAttribute();
            if (parameter == BootstrapCtxParameters.NAME) {
                resourceRegistration.registerReadOnlyAttribute(ad, ReadResourceNameOperationStepHandler.INSTANCE);
            } else {
            resourceRegistration.registerReadWriteAttribute(ad, null, new ReloadRequiredWriteAttributeHandler(ad));
            }
        }

    }


    public enum BootstrapCtxParameters {
        NAME(SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING)
                .setAllowExpression(false)
                .setRequired(true)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("name")
                .build()),
        WORKMANAGER(SimpleAttributeDefinitionBuilder.create("workmanager", ModelType.STRING)
                .setAllowExpression(true)
                .setRequired(true)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("workmanager")
                .build());


        BootstrapCtxParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }
}
