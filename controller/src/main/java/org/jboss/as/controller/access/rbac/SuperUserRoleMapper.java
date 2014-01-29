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

package org.jboss.as.controller.access.rbac;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizerConfiguration;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;

/**
 * A {@link RoleMapper} that always maps the user to the role SUPERUSER.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SuperUserRoleMapper implements RoleMapper {

    private final Set<String> SUPERUSER = Collections.singleton(StandardRole.SUPERUSER.toString());

    private final AuthorizerConfiguration authorizerConfiguration;
    public SuperUserRoleMapper(AuthorizerConfiguration configuration) {
        authorizerConfiguration = configuration;
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetAttribute attribute) {
        return SUPERUSER;
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetResource resource) {
        return SUPERUSER;
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Set<String> operationHeaderRoles) {
        return SUPERUSER;
    }

    @Override
    public boolean canRunAs(Set<String> mappedRoles, String runAsRole) {
        if (runAsRole == null) {
            return false;
        }

        boolean hasRole = authorizerConfiguration.hasRole(runAsRole);
        boolean isSuperUser = mappedRoles.contains(StandardRole.SUPERUSER.toString());

        /*
         * We only allow users to specify roles to run as if they are SuperUser, if the user is not SuperUser we need to be
         * careful to not provide a way for the user to test which roles actually exist.
         */

        if (isSuperUser && hasRole == false) {
            throw ControllerMessages.MESSAGES.unknownRole(runAsRole);
        }

        return hasRole && isSuperUser;
    }

}
