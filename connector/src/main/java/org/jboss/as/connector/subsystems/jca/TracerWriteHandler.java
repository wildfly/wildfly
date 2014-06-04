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

/**
 * {@link org.jboss.as.controller.OperationStepHandler} for the {@code write-attribute} operation for the
 * {@link org.jboss.as.connector.subsystems.jca.TracerDefinition tracer resource}.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class TracerWriteHandler extends AbstractWriteAttributeHandler<JcaSubsystemConfiguration> {

    static TracerWriteHandler INSTANCE = new TracerWriteHandler();

    private TracerWriteHandler() {
        super(TracerDefinition.TracerParameters.TRACER_ENABLED.getAttribute());
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<JcaSubsystemConfiguration> jcaSubsystemConfigurationHandbackHolder) throws OperationFailedException {
        JcaSubsystemConfiguration config = (JcaSubsystemConfiguration) context.getServiceRegistry(true).getService(ConnectorServices.CONNECTOR_CONFIG_SERVICE).getValue();

        if (attributeName.equals(TracerDefinition.TracerParameters.TRACER_ENABLED.getAttribute().getName())) {
            config.setTracer(resolvedValue.asBoolean());
        }

        return false;

    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, JcaSubsystemConfiguration handback) throws OperationFailedException {
        JcaSubsystemConfiguration config = (JcaSubsystemConfiguration) context.getServiceRegistry(true).getService(ConnectorServices.CONNECTOR_CONFIG_SERVICE).getValue();

        if (attributeName.equals(TracerDefinition.TracerParameters.TRACER_ENABLED.getAttribute().getName())) {
            config.setTracer(valueToRestore.asBoolean());
        }
    }
}
