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

import org.jboss.as.controller.PathAddress;

/**
 * Encapsulates information about the relationship of a resource to server groups in a domain.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public interface ServerGroupEffect {

    /**
     * Gets the address of the resource whose server group relationships are described
     * by this object.
     *
     * @return the resource address. Will not be {@code null}
     */
    PathAddress getResourceAddress();

    /**
     * Gets whether the resource logically affects all server groups, including server groups that may not presently exist.
     * <p>
     * A resource that by its nature does not automatically have the potential to affect all server groups will
     * not return {@code true} even if currently all server groups that currently exist are affected by it. For
     * example, in a domain with a single profile and a single server group, that profile affects all server groups.
     * But a profile does not by its nature affect all server groups, and a new server group could be added that
     * does not use the profile. So in that case this method will return {@code false}.
     * </p>
     * <p>
     * Conversely, many host level resources (e.g. {@code /host=x/system-property=y)} only affect the servers managed
     * by that host, and thus could be said to only affect those servers' server groups. However, a new server could be
     * added to that host, with a new server group mapping, and once that is done the host level resource will affect
     * that server group. So, for that host level resource this method will return {@code true}.
     * </p>
     *
     * @return {@code true} if the resource logically affects all server groups; {@code false} if the resource is
     *         logically limited to a set of server groups.
     */
    boolean isServerGroupEffectGlobal();

    /**
     * Gets whether the resource logically only affects certain server groups but hasn't been specifically associated
     * with a particular server group.
     *
     * @return @{code true} if the resource relates to server groups but has not been assigned to any
     */
    boolean isServerGroupEffectUnassigned();

    /**
     * Gets the names of the server groups affected by this resource, or {@code null} if
     * {@link #isServerGroupEffectGlobal()} returns {@code true}
     *
     * @return the names of the server groups, or {@code null} if {@link #isServerGroupEffectGlobal()} returns
     *         {@code true}. Will not return {@code null} if {@link #isServerGroupEffectGlobal()} returns
     *         {@code false}, although it may return an empty set.
     */
    Set<String> getAffectedServerGroups();

    /**
     * Gets whether this effect adds the affected server groups.
     *
     * @return {@code true} if the effect is to add the affected groups.
     */
    boolean isServerGroupAdd();

    /**
     * Gets whether this effect removes the affected server groups.
     *
     * @return {@code true} if the effect is to remove the affected groups.
     */
    boolean isServerGroupRemove();
}
