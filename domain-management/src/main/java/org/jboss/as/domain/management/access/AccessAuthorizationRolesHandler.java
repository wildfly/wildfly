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

import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.access.AuthorizerConfiguration;
import org.jboss.dmr.ModelNode;

/**
 * Handlers for reading the lists of roles from the {@link AuthorizerConfiguration}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
abstract class AccessAuthorizationRolesHandler implements OperationStepHandler {

    static AccessAuthorizationRolesHandler getStandardRolesHandler(AuthorizerConfiguration authorizerConfiguration) {
        return new Standard(authorizerConfiguration);
    }

    static AccessAuthorizationRolesHandler getAllRolesHandler(AuthorizerConfiguration authorizerConfiguration) {
        return new All(authorizerConfiguration);
    }

    final AuthorizerConfiguration authorizerConfiguration;

    AccessAuthorizationRolesHandler(AuthorizerConfiguration configuration) {
        authorizerConfiguration = configuration;
    }


    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        Set<String> list = getRolesList();
        ModelNode result = context.getResult().setEmptyList();
        for (String role : list) {
            result.add(role);
        }
        context.stepCompleted();
    }

    abstract Set<String> getRolesList();

    private static class Standard extends AccessAuthorizationRolesHandler {

        private Standard(AuthorizerConfiguration configuration) {
            super(configuration);
        }

        @Override
        Set<String> getRolesList() {
            return authorizerConfiguration.getStandardRoles();
        }
    }

    private static class All extends AccessAuthorizationRolesHandler {

        private All(AuthorizerConfiguration configuration) {
            super(configuration);
        }

        @Override
        Set<String> getRolesList() {
            return authorizerConfiguration.getAllRoles();
        }
    }
}
