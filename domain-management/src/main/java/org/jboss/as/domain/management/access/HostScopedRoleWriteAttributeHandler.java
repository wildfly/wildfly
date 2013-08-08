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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.constraint.HostEffectConstraint;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * Handles the {@code write-attribute} operation for a {@link HostScopedRolesResourceDefinition host scoped role}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
class HostScopedRoleWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    private final Map<String, HostEffectConstraint> constraintMap;

    HostScopedRoleWriteAttributeHandler(Map<String, HostEffectConstraint> constraintMap) {
        super(HostScopedRolesResourceDefinition.HOSTS);
        this.constraintMap = constraintMap;
    }


    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {

        applyChangeToConstraint(operation, resolvedValue);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        applyChangeToConstraint(operation, valueToRestore);
    }

    private void applyChangeToConstraint(final ModelNode operation, final ModelNode resolvedValue) {

        final String roleName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        HostEffectConstraint constraint = constraintMap.get(roleName);

        // null means the resource shouldn't exist and we should have failed in Stage.MODEL
        assert constraint != null : "unknown role " + roleName;

        List<String> hosts = new ArrayList<String>();
        if (resolvedValue.isDefined()) {
            for (ModelNode host : resolvedValue.asList()) {
                hosts.add(host.asString());
            }
        }

        constraint.setAllowedHosts(hosts);
    }


}

