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

package org.jboss.as.controller.access;

import java.util.Set;

/**
 * Interface exposed by the enforcement point in a WildFly access control system.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public interface Authorizer {

    /**
     * Gets a description of the characteristics of this authorizer
     *
     * @return the description. Cannot be {@code null}
     */
    AuthorizerDescription getDescription();

    /**
     * Authorize a management operation affecting an individual attribute.
     *
     * @param caller the caller. Cannot be {@code null}
     * @param callEnvironment  the call environment. Cannot be {@code null}
     * @param action the action being authorized. Cannot be {@code null}
     * @param target the target of the action. Cannot be {@code null}
     * @return the authorization result. Will not be {@code null}
     */
    AuthorizationResult authorize(Caller caller, Environment callEnvironment, Action action, TargetAttribute target);

    /**
     * Authorize a management operation affecting an entire resource.
     *
     * @param caller the caller. Cannot be {@code null}
     * @param callEnvironment  the call environment. Cannot be {@code null}
     * @param action the action being authorized. Cannot be {@code null}
     * @param target the target of the action. Cannot be {@code null}
     * @return the authorization result. Will not be {@code null}
     */
    AuthorizationResult authorize(Caller caller, Environment callEnvironment, Action action, TargetResource target);

    /**
     * Authorize a JMX operation. This operation should NOT be called for the non-management facade MBeans
     *
     * @param caller the caller. Cannot be {@code null}
     * @param callEnvironment the call environment. Cannot be {@code null}
     * @param action the action being authorized. Cannot be {@code null}
     * @return the authorization result. Will not be {@code null}
     */
    AuthorizationResult authorizeJmxOperation(Caller caller, Environment callEnvironment, JmxAction action);

    /**
     * Gets the set of roles the caller can run as taking into account any requested 'run as' roles.
     *
     *
     * @param caller the caller. Cannot be {@code null}
     * @param callEnvironment the call environment. Cannot be {@code null}
     * @param runAsRoles any requested 'run as' roles. May be {@code null}
     *
     * @return The set of roles assigned to the caller; an empty set will be returned if no roles are assigned or {@code null}
     *         will be returned if the access control provider does not support role mapping.
     */
    Set<String> getCallerRoles(Caller caller, Environment callEnvironment, Set<String> runAsRoles);

    /**
     * Description of standard information about the custom authorizer.
     */
    interface AuthorizerDescription {

        /**
         * Gets whether the authorizer uses a role-based authorization mechanism.
         *
         * @return {@code true} if a role-based mechanism is used; {@code false} if not
         */
        boolean isRoleBased();

        /**
         * Gets the names of the "standard" built-in roles used by the authorizer. A built-in role requires no
         * end user configuration.
         *
         * @return the standard role names. Will not be {@code null}, but may be an empty set if roles are not used
         *         or no built-in roles are used.
         */
        Set<String> getStandardRoles();

    }
}
