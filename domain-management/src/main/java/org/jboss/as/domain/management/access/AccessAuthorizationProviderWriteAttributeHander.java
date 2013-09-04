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
import org.jboss.as.controller.access.Authorizer;
import org.jboss.as.controller.access.AuthorizerConfiguration;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.access.rbac.RoleMapper;
import org.jboss.as.controller.access.rbac.StandardRBACAuthorizer;
import org.jboss.as.controller.access.rbac.StandardRoleMapper;
import org.jboss.as.controller.access.rbac.SuperUserRoleMapper;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.access.AccessAuthorizationResourceDefinition.Provider;
import org.jboss.dmr.ModelNode;

/**
 * An {@link org.jboss.as.controller.OperationStepHandler} handling write updates to the 'provider' attribute allowing
 * for the authorization provider to be switched.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class AccessAuthorizationProviderWriteAttributeHander extends AbstractWriteAttributeHandler<Void> {

    private final DelegatingConfigurableAuthorizer configurableAuthorizer;

    AccessAuthorizationProviderWriteAttributeHander(DelegatingConfigurableAuthorizer configurableAuthorizer) {
        super(AccessAuthorizationResourceDefinition.PROVIDER);
        this.configurableAuthorizer = configurableAuthorizer;
    }


    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
            ModelNode oldValue, Resource model) throws OperationFailedException {
         Provider provider = Provider.valueOf(newValue.asString().toUpperCase(Locale.ENGLISH));
         if (provider == Provider.RBAC) {
             /*
              * As the provider is being set to RBAC we need to be sure roles can be assigned.
              */
             RbacSanityCheckOperation.addOperation(context);
         }
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
        if (!resolvedValue.equals(currentValue)) {
            if (!context.isBooting()) {
                return true;
            }
            updateAuthorizer(resolvedValue, configurableAuthorizer);
        }

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        updateAuthorizer(valueToRestore, configurableAuthorizer);
    }

    static void updateAuthorizer(final ModelNode value, final DelegatingConfigurableAuthorizer configurableAuthorizer) {
        ModelNode resolvedValue = value.isDefined() ? value : AccessAuthorizationResourceDefinition.PROVIDER.getDefaultValue();
        String providerName = resolvedValue.asString().toUpperCase(Locale.ENGLISH);
        Provider provider = Provider.valueOf(providerName);
        AuthorizerConfiguration authorizerConfiguration = configurableAuthorizer.getWritableAuthorizerConfiguration();
        RoleMapper roleMapper;
        if (provider == Provider.SIMPLE) {
            roleMapper = new SuperUserRoleMapper(authorizerConfiguration);
        } else {
            roleMapper = new StandardRoleMapper(configurableAuthorizer.getWritableAuthorizerConfiguration());
        }
        Authorizer delegate = StandardRBACAuthorizer.create(configurableAuthorizer.getWritableAuthorizerConfiguration(),
                roleMapper);
        configurableAuthorizer.setDelegate(delegate);
    }

}
