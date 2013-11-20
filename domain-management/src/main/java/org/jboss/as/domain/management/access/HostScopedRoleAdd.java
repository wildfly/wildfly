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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.access.AuthorizerConfiguration;
import org.jboss.as.controller.access.constraint.HostEffectConstraint;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Handles the {@code add} operation for a {@link HostScopedRolesResourceDefinition host scoped role}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
class HostScopedRoleAdd extends ScopedRoleAddHandler {

    private final Map<String, HostEffectConstraint> constraintMap;
    private final WritableAuthorizerConfiguration authorizerConfiguration;

    HostScopedRoleAdd(Map<String, HostEffectConstraint> constraintMap, WritableAuthorizerConfiguration authorizerConfiguration) {
        super(authorizerConfiguration, HostScopedRolesResourceDefinition.BASE_ROLE, HostScopedRolesResourceDefinition.HOSTS);
        this.constraintMap = constraintMap;
        this.authorizerConfiguration = authorizerConfiguration;
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {

        String roleName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

        String baseRole = HostScopedRolesResourceDefinition.BASE_ROLE.resolveModelAttribute(context, model).asString();

        ModelNode hostsAttribute = HostScopedRolesResourceDefinition.HOSTS.resolveModelAttribute(context, model);
        List<ModelNode> nodeList = hostsAttribute.isDefined() ? hostsAttribute.asList() : Collections.<ModelNode>emptyList();

        addScopedRole(roleName, baseRole, nodeList, authorizerConfiguration, constraintMap);
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {

        String roleName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        authorizerConfiguration.removeScopedRole(roleName);
        constraintMap.remove(roleName);
    }

    static void addScopedRole(final String roleName, final String baseRole, final List<ModelNode> hostNodes,
                              final WritableAuthorizerConfiguration authorizerConfiguration,
                              final Map<String, HostEffectConstraint> constraintMap) {

        List<String> hosts = new ArrayList<String>();
        for (ModelNode host : hostNodes) {
            hosts.add(host.asString());
        }
        HostEffectConstraint constraint = new HostEffectConstraint(hosts);
        authorizerConfiguration.addScopedRole(new AuthorizerConfiguration.ScopedRole(roleName, baseRole, constraint));
        constraintMap.put(roleName, constraint);
    }
}

