/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;

/**
 * Write Handler for jca config attribute
 *
 * @author Stefano Maestri
 */
class JcaAttributeWriteHandler extends AbstractWriteAttributeHandler<JcaSubsystemConfiguration> {

    static JcaAttributeWriteHandler INSTANCE = new JcaAttributeWriteHandler();

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<JcaSubsystemConfiguration> jcaSubsystemConfigurationHandbackHolder) throws OperationFailedException {
        JcaSubsystemConfiguration config = (JcaSubsystemConfiguration) context.getServiceRegistry(false).getService(ConnectorServices.CONNECTOR_CONFIG_SERVICE).getValue();

        CachedConnectionManager ccm = (CachedConnectionManager) context.getServiceRegistry(false).getService(ConnectorServices.CCM_SERVICE).getValue();

        if (attributeName.equals(ArchiveValidationAdd.ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute().getName())) {
            config.setArchiveValidation(resolvedValue.asBoolean());
        }

        if (attributeName.equals(ArchiveValidationAdd.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_ERROR.getAttribute().getName())) {
            config.setArchiveValidationFailOnError(resolvedValue.asBoolean());
        }
        if (attributeName.equals(ArchiveValidationAdd.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_WARN.getAttribute().getName())) {
            config.setArchiveValidationFailOnWarn(resolvedValue.asBoolean());
        }

        if (attributeName.equals(BeanValidationAdd.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().getName())) {
            config.setBeanValidation(resolvedValue.asBoolean());
        }

        if (attributeName.equals(BeanValidationAdd.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().getName())) {
                    config.setBeanValidation(resolvedValue.asBoolean());
        }

        if (attributeName.equals(CachedConnectionManagerAdd.CcmParameters.DEBUG.getAttribute().getName())) {
            ccm.setDebug(resolvedValue.asBoolean());
        }

        if (attributeName.equals(CachedConnectionManagerAdd.CcmParameters.ERROR.getAttribute().getName())) {
            ccm.setError(resolvedValue.asBoolean());
        }


        return false;

    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, JcaSubsystemConfiguration handback) throws OperationFailedException {
        JcaSubsystemConfiguration config = (JcaSubsystemConfiguration) context.getServiceRegistry(false).getService(ConnectorServices.CONNECTOR_CONFIG_SERVICE).getValue();

        CachedConnectionManager ccm = (CachedConnectionManager) context.getServiceRegistry(false).getService(ConnectorServices.CCM_SERVICE).getValue();

        if (attributeName.equals(ArchiveValidationAdd.ArchiveValidationParameters.ARCHIVE_VALIDATION_ENABLED.getAttribute().getName())) {
            config.setArchiveValidation(valueToRestore.asBoolean());
        }

        if (attributeName.equals(ArchiveValidationAdd.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_ERROR.getAttribute().getName())) {
            config.setArchiveValidationFailOnError(valueToRestore.asBoolean());
        }
        if (attributeName.equals(ArchiveValidationAdd.ArchiveValidationParameters.ARCHIVE_VALIDATION_FAIL_ON_WARN.getAttribute().getName())) {
            config.setArchiveValidationFailOnWarn(valueToRestore.asBoolean());
        }

        if (attributeName.equals(BeanValidationAdd.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().getName())) {
            config.setBeanValidation(valueToRestore.asBoolean());
        }

        if (attributeName.equals(BeanValidationAdd.BeanValidationParameters.BEAN_VALIDATION_ENABLED.getAttribute().getName())) {
            config.setBeanValidation(valueToRestore.asBoolean());
        }

        if (attributeName.equals(CachedConnectionManagerAdd.CcmParameters.DEBUG.getAttribute().getName())) {
            ccm.setDebug(valueToRestore.asBoolean());
        }

        if (attributeName.equals(CachedConnectionManagerAdd.CcmParameters.ERROR.getAttribute().getName())) {
            ccm.setError(valueToRestore.asBoolean());
        }

    }
}
