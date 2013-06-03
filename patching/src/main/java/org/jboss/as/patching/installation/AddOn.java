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

package org.jboss.as.patching.installation;

import org.jboss.as.patching.DirectoryStructure;

/**
 *
 * Add-on target info layout:
 *
 * <pre><code>
 *
 * ${JBOSS_HOME}
 * |-- bundles
 * |   `-- system
 * |       `-- add-ons
 * |           `-- &lt;name> => {@link DirectoryStructure#getBundleRepositoryRoot()}
 * |               `-- patches
 * |                   `-- &lt;patchId> => {@link DirectoryStructure#getBundlesPatchDirectory(String)}
 * |-- modules
 * |   `-- system
 * |       `-- add-ons
 * |           `-- &lt;name> => {@link DirectoryStructure#getModuleRoot()}
 * |                `-- patches
 * |                   `-- &lt;patchId> => {@link DirectoryStructure#getModulePatchDirectory(String)}
 * `-- .installation
 *     `-- patches
 *         `-- add-ons
 *             `-- &lt;name>
 *                 |-- cumulative => {@link DirectoryStructure#getCumulativeLink()}
 *                 `-- references
 *                     `-- &lt;patchId> => {@link DirectoryStructure#getCumulativeRefs(String)}
 * <code>
 * </pre>
 *
 * @author Emanuel Muckenhuber
 */
public interface AddOn extends PatchableTarget {

    /**
     * The add-on name.
     *
     * @return the add-on name
     */
    String getName();

}
