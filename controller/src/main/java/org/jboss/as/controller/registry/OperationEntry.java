/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.registry;

import java.util.EnumSet;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;

/**
 * Information about a registered {@code NewStepHandler}.
 *
 * @author Emanuel Muckenhuber
 */
public final class OperationEntry {
    public enum EntryType {
        PUBLIC, PRIVATE;
    }

    /** Flags to indicate special characteristics of an operation */
    public enum Flag {
        /** Operation only reads, does not modify */
        READ_ONLY,
        /** Operation only performs a deployment upload */
        DEPLOYMENT_UPLOAD
    }

    private final OperationStepHandler operationHandler;
    private final DescriptionProvider descriptionProvider;
    private final EntryType type;
    private final EnumSet<Flag> flags;
    private final boolean inherited;

    protected OperationEntry(final OperationStepHandler operationHandler, final DescriptionProvider descriptionProvider, final boolean inherited, final EntryType type, final EnumSet<Flag> flags) {
        this.operationHandler = operationHandler;
        this.descriptionProvider = descriptionProvider;
        this.inherited = inherited;
        this.type = type;
        this.flags = flags;
    }

    protected OperationEntry(final OperationStepHandler operationHandler, final DescriptionProvider descriptionProvider, final boolean inherited, final EntryType type) {
       this(operationHandler, descriptionProvider, inherited, type, EnumSet.noneOf(Flag.class));
    }

    public OperationStepHandler getOperationHandler() {
        return operationHandler;
    }

    public DescriptionProvider getDescriptionProvider() {
        return descriptionProvider;
    }

    public boolean isInherited() {
        return inherited;
    }

    public EntryType getType() {
        return type;
    }

    public EnumSet<Flag> getFlags() {
        return flags == null ? EnumSet.noneOf(Flag.class) : flags.clone();
    }

}
