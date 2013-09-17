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

package org.jboss.as.controller.access.management;

/**
* Unique identifier for an {@link AccessConstraintDefinition}.
*
* @author Brian Stansberry (c) 2013 Red Hat Inc.
*/
public class AccessConstraintKey {
    private final String type;
    private final boolean core;
    private final String subsystem;
    private final String name;

    public AccessConstraintKey(AccessConstraintDefinition definition) {
        this(definition.getType(), definition.isCore(), definition.getSubsystemName(), definition.getName());
    }

    public AccessConstraintKey(String type, boolean core, String subsystem, String name) {
        this.type = type;
        this.core = core;
        this.subsystem = core ? null :subsystem;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccessConstraintKey that = (AccessConstraintKey) o;

        return core == that.core
                && name.equals(that.name)
                && !(subsystem != null ? !subsystem.equals(that.subsystem) : that.subsystem != null)
                && type.equals(that.type);

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{type=" + type + ",core=" + core + ",subsystem=" + subsystem + ",name=" + name + '}';
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (core ? 1 : 0);
        result = 31 * result + (subsystem != null ? subsystem.hashCode() : 0);
        result = 31 * result + name.hashCode();
        return result;
    }

    public String getType() {
        return type;
    }

    public boolean isCore() {
        return core;
    }

    public String getSubsystemName() {
        return subsystem;
    }

    public String getName() {
        return name;
    }
}
