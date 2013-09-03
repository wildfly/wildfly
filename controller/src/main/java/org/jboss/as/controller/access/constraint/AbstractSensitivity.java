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
 * Configuration of sensitive data. Typically {@link org.jboss.as.controller.AttributeDefinition}, {@link org.jboss.as.controller.OperationDefinition}
 * and {@link org.jboss.as.controller.ResourceDefinition} will be annotated with zero or more
 * {@link org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition} containing this information. The purpose of this
 * class is to establish a default behaviour regarding sensitivity for
 * <ul>
 *      <li><b>access</b> - to be able to even be aware of the target's existence</li>
 *      <li><b>read</b> - to be able to read the target's data</li>
 *      <li><b>write</b> - to be able to write to the target</li>
 * </ul>
 * when registering a resource, attribute or operation. This default behaviour can then be tweaked.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class AbstractSensitivity {

    /** If {@code true} access (awareness) is considered sensitive by default*/
    private final boolean defaultRequiresAccessPermission;
    /** If {@code true} reading is considered sensitive by default*/
    private final boolean defaultRequiresReadPermission;
    /** If {@code true} writing is considered sensitive by default*/
    private final boolean defaultRequiresWritePermission;
    private volatile Boolean configuredRequiresAccessPermission;
    private volatile Boolean configuredRequiresReadPermission;
    private volatile Boolean configuredRequiresWritePermission;

    protected AbstractSensitivity(boolean defaultRequiresAccessPermission, boolean defaultRequiresReadPermission, boolean defaultRequiresWritePermission) {
        this.defaultRequiresAccessPermission = defaultRequiresAccessPermission;
        this.defaultRequiresReadPermission = defaultRequiresReadPermission;
        this.defaultRequiresWritePermission = defaultRequiresWritePermission;
    }

    public boolean isDefaultRequiresAccessPermission() {
        return defaultRequiresAccessPermission;
    }

    public boolean isDefaultRequiresReadPermission() {
        return defaultRequiresReadPermission;
    }

    public boolean isDefaultRequiresWritePermission() {
        return defaultRequiresWritePermission;
    }

    public boolean getRequiresAccessPermission() {
        final Boolean requires = configuredRequiresAccessPermission;
        return requires == null ? defaultRequiresAccessPermission : requires;
    }

    public Boolean getConfiguredRequiresAccessPermission() {
        return configuredRequiresAccessPermission;
    }

    public void setConfiguredRequiresAccessPermission(Boolean requiresAccessPermission) {
        this.configuredRequiresAccessPermission = requiresAccessPermission;
    }

    public boolean getRequiresReadPermission() {
        final Boolean requires = configuredRequiresReadPermission;
        return requires == null ? defaultRequiresReadPermission : requires;
    }

    public Boolean getConfiguredRequiresReadPermission() {
        return configuredRequiresReadPermission;
    }

    public void setConfiguredRequiresReadPermission(Boolean requiresReadPermission) {
        this.configuredRequiresReadPermission = requiresReadPermission;
    }

    public boolean getRequiresWritePermission() {
        final Boolean requires = configuredRequiresWritePermission;

        return requires == null ? defaultRequiresWritePermission : requires;
    }

    public Boolean getConfiguredRequiresWritePermission() {
        return configuredRequiresWritePermission;
    }

    public boolean isSensitive(Action.ActionEffect actionEffect) {
        if (actionEffect == Action.ActionEffect.ADDRESS) {
            return getRequiresAccessPermission();
        } else if (actionEffect == Action.ActionEffect.READ_CONFIG || actionEffect == Action.ActionEffect.READ_RUNTIME) {
            return getRequiresReadPermission();
        } else {
            return getRequiresWritePermission();
        }
    }

    public void setConfiguredRequiresWritePermission(Boolean requiresWritePermission) {
        this.configuredRequiresWritePermission = requiresWritePermission;
    }

    protected boolean isCompatibleWith(AbstractSensitivity other) {
        return !equals(other) ||
                (defaultRequiresAccessPermission == other.defaultRequiresAccessPermission
                        && defaultRequiresReadPermission == other.defaultRequiresReadPermission
                        && defaultRequiresWritePermission == other.defaultRequiresWritePermission);
    }
}
