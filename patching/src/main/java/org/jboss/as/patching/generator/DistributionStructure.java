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

package org.jboss.as.patching.generator;

/**
 * Encapsulates rules about the structure of a given standard unzipped distribution of the application server.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public interface DistributionStructure {
    /**
     * See if this content path is ignored.
     *
     * @param item the content item
     * @return {@code true} if this path should be ignored, {@code false} otherwise
     */
    boolean isIgnored(DistributionContentItem item);

    /**
     * Check if this path is a module root.
     *
     * @param item the content item
     * @return {@code true} if this path should be treated as module root
     */
    boolean isModuleLookupPath(DistributionContentItem item);

    /**
     * Check if this path is a bundle root.
     *
     * @param item the content item
     * @return {@code true} if this path should be treated as bundle root
     */
    boolean isBundleLookupPath(DistributionContentItem item);

    /**
     * Register a module search path.
     *
     * @param name         logical name for the path
     * @param standardPath the path
     */
    void registerStandardModuleSearchPath(String name, String standardPath);

    /**
     * Configures this structure to no include the default module search path (modules/) as a valid path.
     */
    void excludeDefaultModuleRoot();

    /**
     * Register a bundle search path.
     *
     * @param name         logical name for the path
     * @param standardPath the path
     */
    void registerStandardBundleSearchPath(String name, String standardPath);

    /**
     * Configures this structure to no include the default bundle search path (bundles/) as a valid path.
     */
    void excludeDefaultBundleRoot();

    /**
     * Register a path that should be ignored when analyzing the structure for differences.
     *
     * @param path the path
     */
    void registerIgnoredPath(String path);

    /**
     * Gets whether this {@code DistributionStructure} is compatible with another distribution structure. Compatible
     * structures will, immediately after construction, have the same default bundle and module paths, the same
     * default ignored paths, and the same rules for determining the types of content items.
     *
     * @param other the other structure
     * @return {@code true} if the structures are compatible, {@code false} otherwise
     */
    boolean isCompatibleWith(DistributionStructure other);

}
