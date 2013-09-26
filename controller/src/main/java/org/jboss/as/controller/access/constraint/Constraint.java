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

/**
 * A constraint that can help govern whether access is allowed.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public interface Constraint {

    /**
     * Gets whether this constraint violates another constraint
     *
     * @param other the other constraint
     * @param actionEffect the effect being evaluated
     *
     * @return {@code true} if the combination of constraints is a violation
     */
    boolean violates(Constraint other, Action.ActionEffect actionEffect);

    /**
     * Gets whether this constraint is equivalent to and can thus replace another constraint
     * in a {@link org.jboss.as.controller.access.permission.ManagementPermission}.
     *
     * @param other the other constraint
     * @return {@code true} if replacement is valid
     */
    boolean replaces(Constraint other);
}
