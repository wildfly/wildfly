/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.RollbackHandler;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * A {@link OperationStepHandler} for adding a role mapping.
 *
 * Initially this is just creating the resource in the model but will be updated later for additional functionality.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RoleMappingAdd implements OperationStepHandler {

    private final WritableAuthorizerConfiguration authorizerConfiguration;
    private final boolean domainMode;

    private RoleMappingAdd(final WritableAuthorizerConfiguration authorizerConfiguration, final boolean domainMode) {
        this.authorizerConfiguration = authorizerConfiguration;
        this.domainMode = domainMode;
    }

    public static OperationStepHandler create(final WritableAuthorizerConfiguration authorizerConfiguration, final boolean domainMode) {
        return new RoleMappingAdd(authorizerConfiguration, domainMode);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String roleName = address.getLastElement().getValue();

        if (authorizerConfiguration.getStandardRoles().contains(roleName) == false) {
            if (domainMode) {
                ScopedRoleRequiredHandler.addOperation(context, roleName);
            } else {
                // Standalone mode so no scoped roles so if it is not a standard role it is invalid.
                throw MESSAGES.invalidRoleName(roleName);
            }
        }

        ModelNode model = resource.getModel();
        RoleMappingResourceDefinition.INCLUDE_ALL.validateAndSet(operation, model);

        registerRuntimeAdd(context, roleName.toUpperCase(Locale.ENGLISH));

        context.stepCompleted();
    }

    private void registerRuntimeAdd(final OperationContext context, final String roleName) {
        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                if (context.isBooting()) {
                    authorizerConfiguration.addRoleMappingImmediate(roleName);

                } else {
                    authorizerConfiguration.addRoleMapping(roleName);
                }

                ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                boolean includeAll = RoleMappingResourceDefinition.INCLUDE_ALL.resolveModelAttribute(context, model).asBoolean();
                if (includeAll) {
                    authorizerConfiguration.setRoleMappingIncludeAll(roleName, includeAll);
                }

                registerRollbackHandler(context, roleName);
            }
        }, Stage.RUNTIME);
    }

    private void registerRollbackHandler(final OperationContext context, final String roleName) {
        context.completeStep(new RollbackHandler() {

            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                Object undoKey = authorizerConfiguration.removeRoleMapping(roleName);

                if (undoKey == null) {
                    // Despite being added the role could not be removed.
                    context.restartRequired();
                }
            }
        });
    }

}
