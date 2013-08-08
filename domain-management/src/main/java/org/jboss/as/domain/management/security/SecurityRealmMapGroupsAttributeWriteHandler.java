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

package org.jboss.as.domain.management.security;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.access.RbacSanityCheckOperation;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * An {@link org.jboss.as.controller.OperationStepHandler} for updates to the map-groups-to-roles attribute.
 *
 * A restart is required as this change is made as the security realm is started - however validation for RBAC is also required.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityRealmMapGroupsAttributeWriteHandler extends ReloadRequiredWriteAttributeHandler {

    public static final SecurityRealmMapGroupsAttributeWriteHandler INSTANCE = new SecurityRealmMapGroupsAttributeWriteHandler();

    private SecurityRealmMapGroupsAttributeWriteHandler() {
        super(SecurityRealmResourceDefinition.MAP_GROUPS_TO_ROLES);
    }

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
            ModelNode oldValue, Resource model) throws OperationFailedException {
        if ((oldValue.equals(newValue) == false) && newValue.isDefined() &&
                (newValue.getType() == ModelType.EXPRESSION || newValue.asBoolean() == false)) {
            RbacSanityCheckOperation.registerOperation(context);
        }
        super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
    }

}
