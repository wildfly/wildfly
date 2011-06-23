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

import org.jboss.as.controller.OperationStepHandler;

/**
 * Information about handling an attribute in a sub-model.
 *
 * @author Brian Stansberry
 */
public final class AttributeAccess {

    /**
     * Indicates how an attributed is accessed.
     */
    public static enum AccessType {
        /** A read-only attribute, which can be either {@code Storage.CONFIGURATION} or {@code Storage.RUNTIME} */
        READ_ONLY("read-only"),
        /** A read-write attribute, which can be either {@code Storage.CONFIGURATION} or {@code Storage.RUNTIME} */
        READ_WRITE("read-write"),
        /** A read-only {@code Storage.RUNTIME} attribute */
        METRIC("metric");

        private final String label;

        private AccessType(final String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Indicates whether an attribute is derived from the persistent configuration or is a purely runtime attribute.
     */
    public static enum Storage {
        /**
         * An attribute whose value is stored in the persistent configuration.
         * The value may also be stored in runtime services.
         */
        CONFIGURATION("configuration"),
        /**
         * An attribute whose value is only stored in runtime services, and
         * isn't stored in the persistent configuration.
         */
        RUNTIME("runtime");

        private final String label;

        private Storage(final String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

    }

    private final AccessType access;
    private final Storage storage;
    private final OperationStepHandler readHandler;
    private final OperationStepHandler writeHandler;

    public AttributeAccess(final AccessType access, final Storage storage, final OperationStepHandler readHandler, final OperationStepHandler writeHandler) {
        assert access != null : "access is null";
        assert storage != null : "storage is null";
        this.access = access;
        this.readHandler = readHandler;
        this.writeHandler = writeHandler;
        this.storage = storage;
        if(access == AccessType.READ_WRITE && writeHandler == null) {
            throw new IllegalArgumentException("writeHandler is null");
        }
    }

    /**
     * Get the access type.
     *
     * @return the access type
     */
    public AccessType getAccessType() {
        return access;
    }

    /**
     * Get the storage type.
     *
     * @return the storage type
     */
    public Storage getStorageType() {
        return storage;
    }

    /**
     * Get the read handler.
     *
     * @return the read handler, <code>null</code> if not defined
     */
    public OperationStepHandler getReadHandler() {
        return readHandler;
    }

    /**
     * Get the write handler.
     *
     * @return the write handler, <code>null</code> if not defined.
     */
    public OperationStepHandler getWriteHandler() {
        return writeHandler;
    }

}
