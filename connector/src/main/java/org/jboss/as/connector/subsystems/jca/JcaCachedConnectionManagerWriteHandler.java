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
