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

package org.jboss.as.osgi.service;

import static org.jboss.as.osgi.OSGiMessages.MESSAGES;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jboss.as.server.ServerEnvironment;

/**
 * Provides a precedence-ordered list of locations from which bundles can be loaded based on the given
 * {@link ServerEnvironment}.
 *
 * <p>
 * <strong>Note:</strong> This is an adaptation of the {@code org.jboss.modules.LayeredModulePathFactory}
 * class in jboss-modules, which performs a similar function for module loading.
 * </p>
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class LayeredBundlePathFactory {

    /**
     * Provides a precedence-ordered list of locations from which bundles can be loaded based on the given
     * {@link ServerEnvironment}.
     */
    static List<File> resolveLayeredBundlePath(final ServerEnvironment serverEnvironment) {

        File bundlesDir = serverEnvironment.getBundlesDir();

        if (!bundlesDir.isDirectory())
            throw MESSAGES.illegalStateCannotFindBundleDir(bundlesDir);

        return resolveLayeredBundlePath(bundlesDir);
    }

    /**
     * Inspects each element in the given {@code bundlePath} to see if it includes a {@code layers.conf} file
     * and/or a standard directory structure with child directories {@code system/layers} and, optionally,
     * {@code system/add-ons}. If so, the layers identified in {@code layers.conf} are added to the module path
     *
     * @param bundlePath the filesystem locations that make up the standard bundle path, each of which is to be
     *                   checked for the presence of layers and add-ons
     *
     * @return a new bundle path, including any layers and add-ons, if found
     */
    static List<File> resolveLayeredBundlePath(final File... bundlePath) {

        List<File> layeredPath = new ArrayList<File>();

        for (File file : bundlePath) {
            // Always add the root, as the user may place modules directly in it
            layeredPath.add(file);

            LayersConfig layersConfig = getLayersConfig(file);

            File layersDir = new File(file, layersConfig.getLayersPath());
            if (!layersDir.exists())  {
                if (layersConfig.isConfigured()) {
                    // Bad config from user
                    throw MESSAGES.illegalStateNoLayersDirectoryFound(layersDir);
                }
                // else this isn't a root that has layers and add-ons
                continue;
            }

            boolean validLayers = true;
            List<File> layerFiles = new ArrayList<File>();
            for (String layerName : layersConfig.getLayers()) {
                File layer = new File(layersDir, layerName);
                if (!layer.exists()) {
                    if (layersConfig.isConfigured()) {
                        // Bad config from user
                        throw MESSAGES.illegalStateCannotFindLayer(layerName, layersDir);
                    }
                    // else this isn't a standard layers and add-ons structure
                    validLayers = false;
                    break;
                }
                layerFiles.add(layer);
            }
            if (validLayers) {
                layeredPath.addAll(layerFiles);
                // Now add-ons
                File[] addOns = new File(file, layersConfig.getAddOnsPath()).listFiles();
                if (addOns != null) {
                    for (File addOn : addOns) {
                        if (addOn.isDirectory()) {
                            layeredPath.add(addOn);
                        }
                    }
                }

            }
        }

        return layeredPath;
    }

    private static LayersConfig getLayersConfig(File repoRoot) {
        File layersList = new File(repoRoot, "layers.conf");
        if (!layersList.exists()) {
            return new LayersConfig();
        }
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(layersList), "UTF-8");
            Properties props = new Properties();
            props.load(reader);

            return new LayersConfig(props);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(reader);
        }
    }

    private static void safeClose(Closeable c) {
        if (c != null) try {
            c.close();
        } catch (Throwable ignored) {}
    }

    private static class LayersConfig {

        private static final String DEFAULT_LAYERS_PATH = "system/layers";
        private static final String DEFAULT_ADD_ONS_PATH = "system/add-ons";

        private final boolean configured;
        private final String layersPath;
        private final String addOnsPath;
        private final List<String> layers;

        private LayersConfig() {
            configured = false;
            layersPath = DEFAULT_LAYERS_PATH;
            addOnsPath = DEFAULT_ADD_ONS_PATH;
            layers = Collections.singletonList("base");
        }

        private LayersConfig(Properties properties) {
            configured = true;
            // Possible future enhancement; probably better to use an xml file. This should evolve consistently
            // with what is done in jboss-modules
//            layersPath = properties.getProperty("layers.path", DEFAULT_LAYERS_PATH);
//            addOnsPath = properties.getProperty("add-ons.path", DEFAULT_ADD_ONS_PATH);
//            boolean excludeBase = Boolean.valueOf(properties.getProperty("exclude.base.layer", "false"));
            layersPath = DEFAULT_LAYERS_PATH;
            addOnsPath = DEFAULT_ADD_ONS_PATH;
            boolean excludeBase = false;
            String layersProp = (String) properties.get("layers");
            if (layersProp == null || (layersProp = layersProp.trim()).length() == 0) {
                if (excludeBase) {
                    layers = Collections.emptyList();
                } else {
                    layers = Collections.singletonList("base");
                }
            } else {
                String[] layerNames = layersProp.split(",");
                layers = new ArrayList<String>();
                boolean hasBase = false;
                for (String layerName : layerNames) {
                    if ("base".equals(layerName)) {
                        hasBase = true;
                    }
                    layers.add(layerName);
                }
                if (!hasBase && !excludeBase) {
                    layers.add("base");
                }
            }
        }

        boolean isConfigured() {
            return configured;
        }


        String getLayersPath() {
            return layersPath;
        }

        String getAddOnsPath() {
            return addOnsPath;
        }

        List<String> getLayers() {
            return layers;
        }
    }
}
