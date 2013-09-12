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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.access.CombinationPolicy;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.dmr.ModelNode;

/**
 * An {@link org.jboss.as.controller.OperationStepHandler} handling write updates to the 'permission-combination-policy'
 * attribute.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class AccessAuthorizationCombinationPolicyWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    private final WritableAuthorizerConfiguration authorizerConfiguration;

    AccessAuthorizationCombinationPolicyWriteAttributeHandler(WritableAuthorizerConfiguration authorizerConfiguration) {
        super(AccessAuthorizationResourceDefinition.PERMISSION_COMBINATION_POLICY);
        this.authorizerConfiguration = authorizerConfiguration;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue,
                                           org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Void> handbackHolder)
            throws OperationFailedException {

        updateAuthorizer(resolvedValue, authorizerConfiguration);
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        updateAuthorizer(valueToRestore, authorizerConfiguration);
    }

    static void updateAuthorizer(final ModelNode value, final WritableAuthorizerConfiguration authorizerConfiguration) {
        ModelNode resolvedValue = value.isDefined() ? value : AccessAuthorizationResourceDefinition.PERMISSION_COMBINATION_POLICY.getDefaultValue();
        String policyName = resolvedValue.asString().toUpperCase(Locale.ENGLISH);
        authorizerConfiguration.setPermissionCombinationPolicy(CombinationPolicy.valueOf(policyName));
    }

}

