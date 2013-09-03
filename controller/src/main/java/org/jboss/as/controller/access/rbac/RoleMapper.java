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

import java.util.Set;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;

/**
 * Determines the set of roles applicable for a management request.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public interface RoleMapper {

    /**
     * Determine the roles available for the caller for a management operation affecting an individual attribute.
     *
     * @param caller the caller. Cannot be {@code null}
     * @param callEnvironment  the call environment. Cannot be {@code null}
     * @param action the action being authorized. Cannot be {@code null}
     * @param attribute the target of the action. Cannot be {@code null}
     * @return the roles. Will not be {@code null}, but may be an empty set
     */
    Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetAttribute attribute);

    /**
     * Determine the roles available for the caller for a management operation affecting an entire resource.
     *
     * @param caller the caller. Cannot be {@code null}
     * @param callEnvironment  the call environment. Cannot be {@code null}
     * @param action the action being authorized. Cannot be {@code null}
     * @param resource the target of the action. Cannot be {@code null}
     * @return the roles. Will not be {@code null}, but may be an empty set
     */
    Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetResource resource);

    /**
     * Determine the roles available for the caller without reference to a particular action or target. Note
     * that actually mapping a caller to roles without reference to a particular action or target is not required.
     *
     *
     * @param caller the caller. Cannot be {@code null}
     * @param callEnvironment  the call environment. Cannot be {@code null}
     * @param operationHeaderRoles any roles specified as headers in the operation. May be {@code null}
     * @return the roles. Will not be {@code null}, but may be an empty set
     */
    Set<String> mapRoles(Caller caller, Environment callEnvironment, Set<String> operationHeaderRoles);

    /**
     * Gets whether the given set of mapped roles provides a caller with the privilege to run as the given
     * "{@code runAsRole}".
     * @param mappedRoles a set of roles obtained from a call to one of this mapper's {@code mapRoles} methods
     * @param runAsRole the role the caller wishes to run as
     * @return {@code true} if running as {@code runAsRole} is allowed
     */
    boolean canRunAs(Set<String> mappedRoles, String runAsRole);

}
