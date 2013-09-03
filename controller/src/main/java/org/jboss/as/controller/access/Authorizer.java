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
     * Gets whether the given caller can run in the given role.
     *
     * @param roleName the name of the role. Cannot be {@code null}
     * @param caller the caller. Cannot be {@code null}
     * @param callEnvironment the call environment. Cannot be {@code null}
     * @param operationHeaderRoles any roles specified as headers in the operation. May be {@code null}
     *
     * @return {@code true} if the caller maps to the given role in the given environment. {@code false} if the
     *         caller does not map to the role for whatever reason, including because the authorizer implementation
     *         is not {@link AuthorizerDescription#isRoleBased() role based} or because the implementation does not support mapping roles
     *         without {@link Action}, {@link JmxAction}, {@link TargetResource} and/or {@link TargetAttribute}
     *         information.
     */
    boolean isCallerInRole(String roleName, Caller caller, Environment callEnvironment, Set<String> operationHeaderRoles);

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
