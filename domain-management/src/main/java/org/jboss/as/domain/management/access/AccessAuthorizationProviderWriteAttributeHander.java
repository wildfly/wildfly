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
import org.jboss.as.controller.access.ConfigurableAuthorizer;
import org.jboss.as.controller.access.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.access.SimpleConfigurableAuthorizer;
import org.jboss.as.controller.access.rbac.RoleMapper;
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

    /**
     * The {@link RoleMapper} to use if the provider is switched to rbac.
     */
    private final RoleMapper rbacRoleMapper;

    AccessAuthorizationProviderWriteAttributeHander(DelegatingConfigurableAuthorizer configurableAuthorizer,
            RoleMapper rbacRoleMapper) {
        super(AccessAuthorizationResourceDefinition.PROVIDER);
        this.configurableAuthorizer = configurableAuthorizer;
        this.rbacRoleMapper = rbacRoleMapper;
    }


    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
            ModelNode oldValue, Resource model) throws OperationFailedException {
         Provider provider = Provider.valueOf(newValue.asString());
         if (provider == Provider.RBAC) {
             /*
              * As the provider is being set to RBAC we need to be sure roles can be assigned.
              */
             RbacSanityCheckOperation.registerOperation(context);
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
            updateAuthorizer(resolvedValue);
        }

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        updateAuthorizer(valueToRestore);
    }

    private void updateAuthorizer(final ModelNode value) {
        String providerName = value.asString().toUpperCase(Locale.ENGLISH);
        Provider provider = Provider.valueOf(providerName);
        ConfigurableAuthorizer delegate;
        if (provider == Provider.SIMPLE) {
            delegate = getSimpleAuthorizer();
        } else {
            delegate = getRoleBasedAuthorizer();
        }
        configurableAuthorizer.setDelegate(delegate);
    }

    private ConfigurableAuthorizer getSimpleAuthorizer() {
        return new SimpleConfigurableAuthorizer();
    }

    private ConfigurableAuthorizer getRoleBasedAuthorizer() {
        return new SimpleConfigurableAuthorizer(rbacRoleMapper);
    }

}
