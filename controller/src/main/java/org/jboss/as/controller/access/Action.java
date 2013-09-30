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

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

/**
 * An action for which access control is needed.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public final class Action {

    public static enum ActionEffect {
        /** "Address" a resource, thus confirming the address is valid. All operations have this effect. */
        ADDRESS("address"),
        /** Read the persistent configuration */
        READ_CONFIG("read-config"),
        /** Read runtime state */
        READ_RUNTIME("read-runtime"),
        /** Modify the persistent configuration */
        WRITE_CONFIG("write-config"),
        /** Modify runtime state */
        WRITE_RUNTIME("write-runtime");

        private final String name;

        private ActionEffect(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final List<AccessConstraintDefinition> NO_CONSTRAINTS = Collections.emptyList();

    private final ModelNode operation;
    private final OperationEntry operationEntry;
    private final Set<ActionEffect> actionEffects;

    public Action(ModelNode operation, OperationEntry operationEntry) {
        this.operation = operation;
        this.operationEntry = operationEntry;
        this.actionEffects = Collections.unmodifiableSet(determineActionEffects(operationEntry));
    }

    public Action(ModelNode operation, OperationEntry operationEntry, Set<ActionEffect> effects) {
        this.operation = operation;
        this.operationEntry = operationEntry;
        this.actionEffects = Collections.unmodifiableSet(effects);
    }

    private static Set<ActionEffect> determineActionEffects(OperationEntry operationEntry) {
        if (operationEntry == null) {
            return Collections.emptySet();
        }
        final EnumSet<ActionEffect> result;
        final EnumSet<OperationEntry.Flag> flags = operationEntry.getFlags();
        if (flags.contains(OperationEntry.Flag.RUNTIME_ONLY)) {
            result = EnumSet.of(ActionEffect.ADDRESS, ActionEffect.READ_RUNTIME);
            if (!flags.contains(OperationEntry.Flag.READ_ONLY)) {
                result.add(ActionEffect.WRITE_RUNTIME);
            }
        } else if (flags.contains(OperationEntry.Flag.READ_ONLY)) {
            result = EnumSet.of(ActionEffect.ADDRESS, ActionEffect.READ_CONFIG, ActionEffect.READ_RUNTIME);
        } else {
            result = EnumSet.allOf(ActionEffect.class);
        }
        return result;
    }

    public ModelNode getOperation() {
        return operation;
    }

    public Set<ActionEffect> getActionEffects() {
        return actionEffects;
    }

    public EnumSet<OperationEntry.Flag> getFlags() {
        return operationEntry != null ? operationEntry.getFlags() : EnumSet.noneOf(OperationEntry.Flag.class);
    }

    public List<AccessConstraintDefinition> getAccessConstraints() {
        return operationEntry != null ? operationEntry.getAccessConstraints() : NO_CONSTRAINTS;
    }

    public Action limitAction(ActionEffect requiredEffect) {
        if (actionEffects.contains(requiredEffect) && actionEffects.size() == 1) {
            return this;
        } else {
            return new Action(operation, operationEntry, EnumSet.of(requiredEffect));
        }
    }
}
