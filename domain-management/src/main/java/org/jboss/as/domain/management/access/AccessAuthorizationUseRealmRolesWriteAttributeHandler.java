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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * An {@link org.jboss.as.controller.OperationStepHandler} for updates to the use-realm-roles attribute and to pass the current value on to the
 * {@link org.jboss.as.controller.access.rbac.StandardRoleMapper}.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AccessAuthorizationUseRealmRolesWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    private final WritableAuthorizerConfiguration authorizerConfiguration;

    AccessAuthorizationUseRealmRolesWriteAttributeHandler(WritableAuthorizerConfiguration authorizerConfiguration) {
        super(AccessAuthorizationResourceDefinition.USE_REALM_ROLES);
        this.authorizerConfiguration = authorizerConfiguration;
    }

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
            ModelNode oldValue, Resource model) throws OperationFailedException {
        if (newValue.getType() == ModelType.EXPRESSION || newValue.asBoolean(false) == false) {
            /*
             * Using roles from the realm has been disabled so now need to check if there that RBAC has been disabled or an
             * alternative mapping strategy is in place.
             */
            RbacSanityCheckOperation.registerOperation(context);
        }
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode resolvedValue, ModelNode currentValue,
            org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Void> handbackHolder)
            throws OperationFailedException {
        setUseRealmRoles(resolvedValue);
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        setUseRealmRoles(valueToRestore);
    }

    private void setUseRealmRoles(final ModelNode value) {
        authorizerConfiguration.setUseRealmRoles(value.asBoolean());
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true; // TODO - May need to be reduced down but for now assume all processes could be using this.
    }

}
