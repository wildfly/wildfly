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

package org.jboss.as.patching.structure;

import org.jboss.as.patching.Constants;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class InstalledIdentity {

    /**
     * Get a list of installed layers.
     *
     * @return the installed layers
     */
    public abstract List<Layer> getLayers();

    /**
     * Get the base layer.
     *
     * @return the base
     */
    public abstract BaseLayer getBaseLayer();

    /**
     * Get a list of installed add-ons.
     *
     * @return the installed add-ons
     */
    public abstract Collection<AddOn> getAddOns();

    /**
     * Load the layers based on the default setup.
     *
     * @param jbossHome the jboss home directory
     * @param repoRoots the repository roots
     * @return the available layers
     * @throws IOException
     */
    public static InstalledIdentity load(final File jbossHome, final File... repoRoots) throws IOException {
        final InstalledImage installedImage = installedImage(jbossHome);
        return load(installedImage, Arrays.<File>asList(repoRoots), Collections.<File>emptyList());
    }

    /**
     * Load the InstalledIdentity configuration based on the module.path
     *
     * @param installedImage the installed image
     * @param moduleRoots    the module roots
     * @param bundleRoots    the bundle roots
     * @return the available layers
     * @throws IOException
     */
    public static InstalledIdentity load(final InstalledImage installedImage, final List<File> moduleRoots, final List<File> bundleRoots) throws IOException {
        return LayersFactory.load(installedImage, moduleRoots, bundleRoots);
    }

    static InstalledImage installedImage(final File jbossHome) {
        final File appClient = new File(jbossHome, Constants.APP_CLIENT);
        final File bundles = new File(jbossHome, Constants.BUNDLES);
        final File domain = new File(jbossHome, Constants.DOMAIN);
        final File modules = new File(jbossHome, Constants.MODULES);
        final File metadata = new File(jbossHome, Constants.INSTALLATION);
        final File layersConf = new File(modules, Constants.LAYERS_CONF);
        final File standalone = new File(jbossHome, Constants.STANDALONE);
        return new InstalledImage() {
            @Override
            public File getJbossHome() {
                return jbossHome;
            }

            @Override
            public File getAppClientDir() {
                return appClient;
            }

            @Override
            public File getBundlesDir() {
                return bundles;
            }

            @Override
            public File getDomainDir() {
                return domain;
            }

            @Override
            public File getInstallationMetadata() {
                return metadata;
            }

            @Override
            public File getModulesDir() {
                return modules;
            }

            @Override
            public File getStandaloneDir() {
                return standalone;
            }

            @Override
            public File getLayersConf() {
                return layersConf;
            }
        };
    }

}
