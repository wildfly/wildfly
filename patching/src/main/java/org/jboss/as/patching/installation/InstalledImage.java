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

import java.io.File;

/**
 * The installed image.
 *
 * @author Emanuel Muckenhuber
 */
public interface InstalledImage {

    /**
     * Get the jboss home.
     *
     * @return the jboss home
     */
    File getJbossHome();

    /**
     * Get the app-client directory.
     *
     * @return the app client dir
     */
    File getAppClientDir();

    /**
     * Get the bundles directory.
     *
     * @return the bundles directory
     */
    File getBundlesDir();

    /**
     * Get the domain directory.
     *
     * @return the domain dir
     */
    File getDomainDir();

    /**
     * Get the installation metadata directory.
     *
     * @return the patches metadata dir
     */
    File getInstallationMetadata();

    /**
     * Get the patch history dir.
     *
     * @param patchId the patch id
     * @return the patch history dir
     */
    File getPatchHistoryDir(String patchId);

    /**
     * Get the patches history root directory.
     *
     * @return the patch root directory
     */
    File getPatchesDir();

    /**
     * Get the modules directory.
     *
     * @return the modules dir
     */
    File getModulesDir();

    /**
     * Get the standalone dir.
     *
     * @return the standalone dir
     */
    File getStandaloneDir();

    /**
     * Get the path to the layers.conf file.
     *
     * @return the layers.conf path
     */
    File getLayersConf();

}
