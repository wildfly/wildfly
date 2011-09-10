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

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ee.component.deployers.DefaultEarSubDeploymentsIsolationProcessor;
import org.jboss.as.ee.structure.GlobalModuleDependencyProcessor;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;
import org.jboss.dmr.ModelNode;

/**
 * Handles the "write-attribute" operation for the EE subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EeWriteAttributeHandler extends ServerWriteAttributeOperationHandler {

    private final DefaultEarSubDeploymentsIsolationProcessor isolationProcessor;
    private final GlobalModuleDependencyProcessor moduleDependencyProcessor;

    public EeWriteAttributeHandler(final DefaultEarSubDeploymentsIsolationProcessor isolationProcessor,
                          final GlobalModuleDependencyProcessor moduleDependencyProcessor) {
        this.isolationProcessor = isolationProcessor;
        this.moduleDependencyProcessor = moduleDependencyProcessor;
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        registry.registerReadWriteAttribute(GlobalModulesDefinition.INSTANCE.getName(), null, this, AttributeAccess.Storage.CONFIGURATION);
        registry.registerReadWriteAttribute(CommonAttributes.EAR_SUBDEPLOYMENTS_ISOLATED.getName(), null, this, AttributeAccess.Storage.CONFIGURATION);
    }

    @Override
    protected void validateValue(String name, ModelNode value) throws OperationFailedException {
        if (GlobalModulesDefinition.INSTANCE.getName().equals(name)) {
            GlobalModulesDefinition.INSTANCE.getValidator().validateParameter(name, value);
        } else if (CommonAttributes.EAR_SUBDEPLOYMENTS_ISOLATED.getName().equals(name)) {
            CommonAttributes.EAR_SUBDEPLOYMENTS_ISOLATED.getValidator().validateParameter(name, value);
        }
    }

    @Override
    protected void validateResolvedValue(String name, ModelNode value) throws OperationFailedException {
        if (GlobalModulesDefinition.INSTANCE.getName().equals(name)) {
            GlobalModulesDefinition.INSTANCE.getValidator().validateResolvedParameter(name, value);
        } else if (CommonAttributes.EAR_SUBDEPLOYMENTS_ISOLATED.getName().equals(name)) {
            CommonAttributes.EAR_SUBDEPLOYMENTS_ISOLATED.getValidator().validateResolvedParameter(name, value);
        }
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode currentValue) throws OperationFailedException {

        applyUpdateToDeploymentUnitProcessor(operation, attributeName);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert) throws OperationFailedException {

        final ModelNode revertOp = operation.clone();
        revertOp.get(attributeName).set(valueToRestore);
        applyUpdateToDeploymentUnitProcessor(revertOp, attributeName);
    }

    private void applyUpdateToDeploymentUnitProcessor(ModelNode operation, String attributeName) throws OperationFailedException {
        if (GlobalModulesDefinition.INSTANCE.getName().equals(attributeName)) {
            final ModelNode globalMods = GlobalModulesDefinition.INSTANCE.validateResolvedOperation(operation);
            moduleDependencyProcessor.setGlobalModules(globalMods);
        } else if (CommonAttributes.EAR_SUBDEPLOYMENTS_ISOLATED.getName().equals(attributeName)) {
            boolean isolate = CommonAttributes.EAR_SUBDEPLOYMENTS_ISOLATED.validateResolvedOperation(operation).asBoolean();
            isolationProcessor.setEarSubDeploymentsIsolated(isolate);
        }
    }
}
