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
 * Information about a registered {@code OperationStepHandler}.
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
        /** Operation only performs a deployment upload
         * @deprecated use {@link #MASTER_HOST_CONTROLLER_ONLY}
         */
        @Deprecated
        DEPLOYMENT_UPLOAD,
        /** The operation modifies the configuration and can be applied to the runtime without requiring a restart */
        RESTART_NONE,
        /** The operation modifies the configuration but can only be applied to the runtime via a full jvm restart */
        RESTART_JVM,
        /** The operation modifies the configuration but can only be applied to the runtime via a restart of all services;
         *  however it does not require a full jvm restart */
        RESTART_ALL_SERVICES,
        /** The operation modifies the configuration but can only be applied to the runtime via a restart of services,
         *  associated with the affected resource, but does not require a restart of all services or a full jvm restart */
        RESTART_RESOURCE_SERVICES,
        /** A domain or host-level operation that should be pushed to the servers even if the default behavior
         *  would indicate otherwise */
        DOMAIN_PUSH_TO_SERVERS,
        /** A host-level operation that should only be executed on the HostController and not on the servers,
         * even if the default behavior would indicate otherwise */
        HOST_CONTROLLER_ONLY,
        /** A domain-level operation that should only be executed on the master HostController and not on the slaves,
         * even if the default behavior would indicate otherwise */
        MASTER_HOST_CONTROLLER_ONLY,
        /** Operations with this flag do not affect the model. The main intention for this is to only make RUNTIME_ONLY methods on
         * domain mode servers visible to end users. */
        RUNTIME_ONLY
    }

    private final OperationStepHandler operationHandler;
    private final DescriptionProvider descriptionProvider;
    private final EntryType type;
    private final EnumSet<Flag> flags;
    private final boolean inherited;

    OperationEntry(final OperationStepHandler operationHandler, final DescriptionProvider descriptionProvider, final boolean inherited, final EntryType type, final EnumSet<Flag> flags) {
        this.operationHandler = operationHandler;
        this.descriptionProvider = descriptionProvider;
        this.inherited = inherited;
        this.type = type;
        this.flags = flags;
    }

    OperationEntry(final OperationStepHandler operationHandler, final DescriptionProvider descriptionProvider, final boolean inherited, final EntryType type) {
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
