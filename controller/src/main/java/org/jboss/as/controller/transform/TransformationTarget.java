/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.extension.SubsystemInformation;

/**
 * A potentially remote target requiring transformation.
 *
 * @author Emanuel Muckenhuber
 */
public interface TransformationTarget {

    /**
     * Get the overall management version string.
     *
     * @return the management version
     */
    String getManagementVersion();

    /**
     * Get the model management major version.
     *
     * @return the management major version
     */
    int getMajorManagementVersion();

    /**
     * Get the model management minor version.
     *
     * @return the management minor version
     */
    int getMinorManagementVersion();

    /**
     * Get the subsystem version.
     *
     * @param subsystemName the subsystem name
     * @return the version of the specified subsystem, {@code null} if it does not exist
     */
    String getSubsystemVersion(String subsystemName);

    /**
     * Get the subsystem information.
     *
     * @param subsystemName the subsystem name
     * @return the subsystem information
     */
    SubsystemInformation getSubsystemInformation(final String subsystemName);

    /**
     * Resolve an operation transformer for a given address.
     *
     * @param address the address
     * @param operationName the operation name
     * @return the operation transformer
     */
    OperationTransformer resolveTransformer(PathAddress address, String operationName);

    SubsystemTransformer getSubsystemTransformer(final String subsystemName);

    /**
     * Return info about need for transformation for this target to take place
     * For instance if target is of same version as current server no need to go trough transformation
     *
     * @return boolean that tells transformers api if transformation should be performed
     */
    boolean isTransformationNeeded();

    /**
     * Add version information for a subsystem.
     *
     * @param subsystemName the name of the subsystem. Cannot be {@code null}
     * @param majorVersion the major version of the subsystem's management API
     * @param minorVersion the minor version of the subsystem's management API
     */
    void addSubsystemVersion(String subsystemName, int majorVersion, int minorVersion);

}
