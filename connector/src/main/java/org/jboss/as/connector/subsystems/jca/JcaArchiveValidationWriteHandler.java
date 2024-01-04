/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Write Handler for Jakarta Connectors config attribute
 *
 * @author Stefano Maestri
 */
class JcaArchiveValidationWriteHandler extends AbstractWriteAttributeHandler<JcaSubsystemConfiguration> {

    static JcaArchiveValidationWriteHandler INSTANCE = new JcaArchiveValidationWriteHandler();

    private JcaArchiveValidationWriteHandler() {
        super(
            JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute(),
            JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_ERROR.getAttribute(),
            JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_WARN.getAttribute()
        );
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<JcaSubsystemConfiguration> jcaSubsystemConfigurationHandbackHolder) throws OperationFailedException {
        JcaSubsystemConfiguration config = (JcaSubsystemConfiguration) context.getServiceRegistry(true).getService(ConnectorServices.CONNECTOR_CONFIG_SERVICE).getValue();

        if (attributeName.equals(JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute().getName())) {
            config.setArchiveValidation(resolvedValue.asBoolean());
        } else if (attributeName.equals(JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_ERROR.getAttribute().getName())) {
            config.setArchiveValidationFailOnError(resolvedValue.asBoolean());
        } else if (attributeName.equals(JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_WARN.getAttribute().getName())) {
            config.setArchiveValidationFailOnWarn(resolvedValue.asBoolean());
        }

        return false;

    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, JcaSubsystemConfiguration handback) throws OperationFailedException {
        JcaSubsystemConfiguration config = (JcaSubsystemConfiguration) context.getServiceRegistry(true).getService(ConnectorServices.CONNECTOR_CONFIG_SERVICE).getValue();

        if (attributeName.equals(JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute().getName())) {
            config.setArchiveValidation(valueToRestore.asBoolean());
        }else if (attributeName.equals(JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_ERROR.getAttribute().getName())) {
            config.setArchiveValidationFailOnError(valueToRestore.asBoolean());
        } else if (attributeName.equals(JcaArchiveValidationDefinition.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_WARN.getAttribute().getName())) {
            config.setArchiveValidationFailOnWarn(valueToRestore.asBoolean());
        }

    }
}
