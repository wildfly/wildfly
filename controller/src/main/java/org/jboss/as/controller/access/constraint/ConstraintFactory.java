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

package org.jboss.as.controller.access.constraint;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 * A factory for constraints.
 * <p>
 * <strong>Implementations of this interface should implement {@link #equals(Object)} and {@link #hashCode()}
 * such that two factories that produce the same constraints can be treated as equal in hash-based collections.</strong>
 * </p>
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public interface ConstraintFactory extends Comparable<ConstraintFactory> {

    /**
     * Provides a constraint suitable for the given {@code role} in the standard WildFly
     * role based access control system.
     *
     * @param role the role
     * @param actionEffect the {@link org.jboss.as.controller.access.Action.ActionEffect} for which the constraint is relevant
     *
     * @return the constraint. Cannot return {@code null}
     */
    Constraint getStandardUserConstraint(StandardRole role, Action.ActionEffect actionEffect);

    /**
     * Provides a constraint appropriate for the given {@code action} and {@code target}
     *
     *
     * @param actionEffect the {@link org.jboss.as.controller.access.Action.ActionEffect} for which the constraint is relevant
     * @param action the action
     * @param target the attribute that is the target of the action
     *
     * @return the constraint. Cannot return {@code null}
     */
    Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetAttribute target);

    /**
     * Provides a constraint appropriate for the given {@code action} and {@code target}
     *
     *
     * @param actionEffect the {@link org.jboss.as.controller.access.Action.ActionEffect} for which the constraint is relevant
     * @param action the action
     * @param target the resource that is the target of the action
     *
     * @return the constraint. Cannot return {@code null}
     */
    Constraint getRequiredConstraint(Action.ActionEffect actionEffect, Action action, TargetResource target);
}
