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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;

/**
 * Internal op called.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class AccessAuthorizationDomainSlaveConfigHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "configure-from-domain";
    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, DomainManagementResolver.getResolver("core.access-control"))
            .withFlag(OperationEntry.Flag.HOST_CONTROLLER_ONLY)
            .setPrivateEntry()
            .build();

    private final DelegatingConfigurableAuthorizer configurableAuthorizer;

    AccessAuthorizationDomainSlaveConfigHandler(DelegatingConfigurableAuthorizer configurableAuthorizer) {
        this.configurableAuthorizer = configurableAuthorizer;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        for (AttributeDefinition ad : AccessAuthorizationResourceDefinition.CONFIG_ATTRIBUTES) {
            ad.validateAndSet(operation, model);
        }
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                WritableAuthorizerConfiguration authorizerConfiguration = configurableAuthorizer.getWritableAuthorizerConfiguration();

                ModelNode provider = AccessAuthorizationResourceDefinition.PROVIDER.resolveModelAttribute(context, model);
                AccessAuthorizationProviderWriteAttributeHander.updateAuthorizer(provider, configurableAuthorizer);
                ModelNode combinationPolicy = AccessAuthorizationResourceDefinition.PERMISSION_COMBINATION_POLICY.resolveModelAttribute(context, model);
                AccessAuthorizationCombinationPolicyWriteAttributeHandler.updateAuthorizer(combinationPolicy, authorizerConfiguration);

                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
