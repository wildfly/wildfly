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

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.ConfigurableAuthorizer;
import org.jboss.as.controller.access.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.access.SimpleConfigurableAuthorizer;
import org.jboss.dmr.ModelNode;

/**
 * TODO class javadoc.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
class AccessControlProviderWriteAttributeHander implements OperationStepHandler {

    private final DelegatingConfigurableAuthorizer configurableAuthorizer;

    AccessControlProviderWriteAttributeHander(DelegatingConfigurableAuthorizer configurableAuthorizer) {
        this.configurableAuthorizer = configurableAuthorizer;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        ModelNode currentValue = AccessControlResourceDefinition.PROVIDER.resolveValue(context, model);
        AccessControlResourceDefinition.PROVIDER.validateAndSet(operation, model);
        final ModelNode newValue = AccessControlResourceDefinition.PROVIDER.resolveValue(context, model);
        if (!newValue.equals(currentValue)) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    if (context.isBooting()) {
                        String providerName = newValue.asString().toUpperCase(Locale.ENGLISH);
                        AccessControlResourceDefinition.Provider provider = AccessControlResourceDefinition.Provider.valueOf(providerName);
                        ConfigurableAuthorizer delegate;
                        if (provider == AccessControlResourceDefinition.Provider.SIMPLE) {
                            delegate = getSimpleAuthorizer();
                        } else {
                            delegate = getRoleBasedAuthorizer();
                        }
                        configurableAuthorizer.setDelegate(delegate);
                        // We don't roll back during boot
                        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                    } else {
                        context.reloadRequired();
                        context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.stepCompleted();
    }

    private ConfigurableAuthorizer getSimpleAuthorizer() {
        return new SimpleConfigurableAuthorizer();
    }

    private ConfigurableAuthorizer getRoleBasedAuthorizer() {
        //TODO implement getRoleBasedAuthorizer
        throw new UnsupportedOperationException();
    }
}
