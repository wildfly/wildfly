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
 * {@link org.jboss.as.controller.OperationStepHandler} for the {@code write-attribute} operation for the
 * {@link JcaArchiveValidationDefinition Jakarta Bean Validation resource}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class JcaBeanValidationWriteHandler extends AbstractWriteAttributeHandler<JcaSubsystemConfiguration> {

    static JcaBeanValidationWriteHandler INSTANCE = new JcaBeanValidationWriteHandler();

    private JcaBeanValidationWriteHandler() {
        super(JcaBeanValidationDefinition.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute());
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<JcaSubsystemConfiguration> jcaSubsystemConfigurationHandbackHolder) throws OperationFailedException {
        JcaSubsystemConfiguration config = (JcaSubsystemConfiguration) context.getServiceRegistry(true).getService(ConnectorServices.CONNECTOR_CONFIG_SERVICE).getValue();

        if (attributeName.equals(JcaBeanValidationDefinition.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().getName())) {
            config.setBeanValidation(resolvedValue.asBoolean());
        }

        return false;

    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, JcaSubsystemConfiguration handback) throws OperationFailedException {
        JcaSubsystemConfiguration config = (JcaSubsystemConfiguration) context.getServiceRegistry(true).getService(ConnectorServices.CONNECTOR_CONFIG_SERVICE).getValue();

        if (attributeName.equals(JcaBeanValidationDefinition.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().getName())) {
            config.setBeanValidation(valueToRestore.asBoolean());
        }
    }
}
