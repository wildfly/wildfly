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
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;

/**
 * {@link org.jboss.as.controller.OperationStepHandler} for the {@code write-attribute} operation for the
 * {@link JcaCachedConnectionManagerDefinition cached connection manager resource}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class JcaCachedConnectionManagerWriteHandler extends AbstractWriteAttributeHandler<JcaSubsystemConfiguration> {

    static JcaCachedConnectionManagerWriteHandler INSTANCE = new JcaCachedConnectionManagerWriteHandler();

    private JcaCachedConnectionManagerWriteHandler() {
        super(
                JcaCachedConnectionManagerDefinition.CcmParameters.DEBUG.getAttribute(),
                JcaCachedConnectionManagerDefinition.CcmParameters.ERROR.getAttribute(),
                JcaCachedConnectionManagerDefinition.CcmParameters.IGNORE_UNKNOWN_CONNECTIONS.getAttribute()
        );
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<JcaSubsystemConfiguration> jcaSubsystemConfigurationHandbackHolder) throws OperationFailedException {

        CachedConnectionManager ccm = (CachedConnectionManager) context.getServiceRegistry(true).getService(ConnectorServices.CCM_SERVICE).getValue();

        if (attributeName.equals(JcaCachedConnectionManagerDefinition.CcmParameters.DEBUG.getAttribute().getName())) {
            ccm.setDebug(resolvedValue.asBoolean());
        } else if (attributeName.equals(JcaCachedConnectionManagerDefinition.CcmParameters.ERROR.getAttribute().getName())) {
            ccm.setError(resolvedValue.asBoolean());
        } else if (attributeName.equals(JcaCachedConnectionManagerDefinition.CcmParameters.IGNORE_UNKNOWN_CONNECTIONS.getAttribute().getName())) {
            ccm.setIgnoreUnknownConnections(resolvedValue.asBoolean());
        }


        return false;

    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, JcaSubsystemConfiguration handback) throws OperationFailedException {

        CachedConnectionManager ccm = (CachedConnectionManager) context.getServiceRegistry(true).getService(ConnectorServices.CCM_SERVICE).getValue();

        if (attributeName.equals(JcaCachedConnectionManagerDefinition.CcmParameters.DEBUG.getAttribute().getName())) {
            ccm.setDebug(valueToRestore.asBoolean());
        } else if (attributeName.equals(JcaCachedConnectionManagerDefinition.CcmParameters.ERROR.getAttribute().getName())) {
            ccm.setError(valueToRestore.asBoolean());
        } else if (attributeName.equals(JcaCachedConnectionManagerDefinition.CcmParameters.IGNORE_UNKNOWN_CONNECTIONS.getAttribute().getName())) {
            ccm.setIgnoreUnknownConnections(valueToRestore.asBoolean());
        }

    }
}
