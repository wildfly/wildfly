/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.Constants.ARCHIVE_VALIDATION;

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
 */
public class JcaArchiveValidationDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_ARCHIVE_VALIDATION = PathElement.pathElement(ARCHIVE_VALIDATION, ARCHIVE_VALIDATION);

    JcaArchiveValidationDefinition() {
        super(PATH_ARCHIVE_VALIDATION,
                JcaExtension.getResourceDescriptionResolver(PATH_ARCHIVE_VALIDATION.getKey()),
                ArchiveValidationAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (final ArchiveValidationParameters parameter : ArchiveValidationParameters.values()) {
            resourceRegistration.registerReadWriteAttribute(parameter.getAttribute(), null, JcaArchiveValidationWriteHandler.INSTANCE);
        }
    }

    public enum ArchiveValidationParameters {
        ARCHIVE_VALIDATION_ENABLED(SimpleAttributeDefinitionBuilder.create("enabled", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(ModelNode.TRUE)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("enabled")
                .build()),
        ARCHIVE_VALIDATION_FAIL_ON_ERROR(SimpleAttributeDefinitionBuilder.create("fail-on-error", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(ModelNode.TRUE)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("fail-on-error")
                .build()),
        ARCHIVE_VALIDATION_FAIL_ON_WARN(SimpleAttributeDefinitionBuilder.create("fail-on-warn", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(ModelNode.FALSE)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("fail-on-warn")
                .build());

        ArchiveValidationParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }

}
