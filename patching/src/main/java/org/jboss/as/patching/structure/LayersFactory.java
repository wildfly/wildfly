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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.jboss.as.patching.Constants.ADD_ONS;
import static org.jboss.as.patching.Constants.BASE;
import static org.jboss.as.patching.Constants.DEFAULT_ADD_ONS_PATH;
import static org.jboss.as.patching.Constants.DEFAULT_BASE_PATH;
import static org.jboss.as.patching.Constants.DEFAULT_LAYERS_PATH;
import static org.jboss.as.patching.Constants.INSTALLATION_METADATA;
import static org.jboss.as.patching.Constants.LAYERS;
import static org.jboss.as.patching.Constants.LAYERS_CONF;
import static org.jboss.as.patching.Constants.NAME;
import static org.jboss.as.patching.Constants.VERSION;
import static org.jboss.as.patching.IoUtils.safeClose;

/**
 * @author Emanuel Muckenhuber
 */
class LayersFactory {

    /**
     * Load the available layers.
     *
     * @param image       the installed image
     * @param moduleRoots the module roots
     * @param bundleRoots the bundle roots
     * @return the layers
     * @throws IOException
     */
    static InstalledIdentity load(final InstalledImage image, final List<File> moduleRoots, final List<File> bundleRoots) throws IOException {

        // Step 1 - gather the installed layers data
        final InstalledConfiguration conf = createInstalledConfig(image);
        // Step 2 - process the actual module and bundle roots
        final ProcessedLayers processedLayers = process(conf, moduleRoots, bundleRoots);
        final InstalledConfiguration config = processedLayers.getConf();

        // Step 3 - create the actual config objects
        final List<Layer> layers = new ArrayList<Layer>();
        final Collection<AddOn> addOns = new ArrayList<AddOn>();
        // Base layer
        final BaseLayer base = createPatchableTarget(BASE, processedLayers.getBase(), config.getBaseMetadataDir());
        // Process layers
        for (final LayerPathConfig layer : processedLayers.getLayers().values()) {
            final String name = layer.name;
            layers.add(createPatchableTarget(name, layer, config.getLayerMetadataDir(name)));
        }
        // Process add-ons
        for (final LayerPathConfig addOn : processedLayers.getAddOns().values()) {
            final String name = addOn.name;
            layers.add(createPatchableTarget(name, addOn, config.getAddOnMetadataDir(name)));
        }
        return new InstalledIdentity() {
            @Override
            public Collection<AddOn> getAddOns() {
                return addOns;
            }

            @Override
            public List<Layer> getLayers() {
                return layers;
            }

            @Override
            public BaseLayer getBaseLayer() {
                return base;
            }
        };
    }

    /**
     * Process the module and bundle roots and cross check with the installed information.
     *
     * @param conf        the installed configuration
     * @param moduleRoots the module roots
     * @param bundleRoots the bundle roots
     * @return the processed layers
     * @throws IOException
     */
    static ProcessedLayers process(final InstalledConfiguration conf, final List<File> moduleRoots, final List<File> bundleRoots) throws IOException {
        final ProcessedLayers layers = new ProcessedLayers(conf);
        // Process module roots
        final LayerPathSetter moduleSetter = new LayerPathSetter() {
            @Override
            public boolean setPath(final LayerPathConfig pending, final File root) {
                if (pending.modulePath == null) {
                    pending.modulePath = root;
                    return true;
                }
                return false;
            }
        };
        for (final File moduleRoot : moduleRoots) {
            processRoot(moduleRoot, layers, moduleSetter);
        }
        // Process bundle root
        final LayerPathSetter bundleSetter = new LayerPathSetter() {
            @Override
            public boolean setPath(LayerPathConfig pending, File root) {
                if (pending.bundlePath == null) {
                    pending.bundlePath = root;
                    return true;
                }
                return false;
            }
        };
        for (final File bundleRoot : bundleRoots) {
            processRoot(bundleRoot, layers, bundleSetter);
        }
        if (conf.getInstalledLayers().size() != layers.getLayers().size()) {
            throw processingError("processed layers don't match expected %s, but was %s", conf.getInstalledLayers(), layers.getLayers().keySet());
        }
        if (conf.getInstalledAddOns().size() != layers.getAddOns().size()) {
            throw processingError("processed add-ons don't match expected %s, but was %s", conf.getInstalledAddOns(), layers.getAddOns().keySet());
        }
        return layers;
    }

    /**
     * Process a module or bundle root.
     *
     * @param root   the root
     * @param layers the processed layers
     * @param setter the bundle or module path setter
     * @throws IOException
     */
    static void processRoot(final File root, final ProcessedLayers layers, final LayerPathSetter setter) throws IOException {
        final LayersConfig layersConfig = getLayersConfig(root);
        // Process base first
        processBase(root, layers, layersConfig, setter);
        // Process layers
        final File layersDir = new File(root, layersConfig.getLayersPath());
        if (!layersDir.exists()) {
            if (layersConfig.isConfigured()) {
                // Bad config from user
                throw processingError("No layers directory found at " + layersDir);
            }
            // else this isn't a root that has layers and add-ons
            return;
        }
        // check for a valid layer configuration
        for (final String layer : layersConfig.getLayers()) {
            if (BASE.equals(layer)) {
                continue; // since we already processed base
            }
            File layerDir = new File(layersDir, layer);
            if (!layerDir.exists()) {
                if (layersConfig.isConfigured()) {
                    // Bad config from user
                    throw processingError("Cannot find layer '%s' under directory %s", layer, layersDir);
                }
                // else this isn't a standard layers and add-ons structure
                return;
            }
            layers.addLayer(layer, layerDir, setter);
        }
        // Finally process the add-ons
        final File addOnsDir = new File(root, layersConfig.getAddOnsPath());
        final File[] addOnsList = addOnsDir.listFiles();
        if (addOnsList != null) {
            for (final File addOn : addOnsList) {
                layers.addAddOn(addOn.getName(), addOn, setter);
            }
        }
    }

    /**
     * Process the system/base path.
     *
     * @param root         the module root
     * @param layers       the processed layers
     * @param layersConfig the layers config
     * @param setter       the module or bundle path setter
     */
    static void processBase(final File root, final ProcessedLayers layers, final LayersConfig layersConfig, final LayerPathSetter setter) {
        final File base = new File(root, DEFAULT_BASE_PATH);
        if (layersConfig.getLayers().contains(BASE)) {
            if (!base.isDirectory()) {
                throw processingError("base does not exist %s", base.getAbsolutePath());
            }
        }
        if (base.isDirectory()) {
            if (!setter.setPath(layers.base, base)) {
                throw processingError("duplicate base directory %s", base.getAbsolutePath());
            }
        }
    }

    /**
     * Create the actual patchable target.
     *
     * @param name     the layer name
     * @param layer    the layer path config
     * @param metadata the metadata location for this target
     * @return the patchable target
     * @throws IOException
     */
    static AbstractPatchableTarget createPatchableTarget(final String name, final LayerPathConfig layer, final File metadata) throws IOException {
        // Load the layer installation metadata
        final File installation = new File(metadata, INSTALLATION_METADATA);
        final Properties properties = loadProperties(installation);
        // Get the name and version
        final String layerName = (String) properties.get(NAME);
        final String layerVersion = (String) properties.get(VERSION);
        if (layerName == null || layerVersion == null) {
            throw processingError("missing attributes in the layer '%s' installation metadata: %s", name, properties);
        }
        // patchable target
        return new AbstractPatchableTarget() {
            @Override
            protected File getModulesBase() {
                if (layer.modulePath == null) {
                    throw new IllegalStateException("no module base available");
                }
                return layer.modulePath;
            }

            @Override
            protected File getBundlesBase() {
                if (layer.bundlePath == null) {
                    throw new IllegalStateException("no bundle base available");
                }
                return layer.bundlePath;
            }

            @Override
            protected File getPatchesMetadataDir() {
                return metadata;
            }

            @Override
            public String getInstallationName() {
                return layerName;
            }

            @Override
            public String getVersion() {
                return layerVersion;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    static class LayerPathConfig {

        File modulePath;
        File bundlePath;

        final String name;

        LayerPathConfig(String name) {
            this.name = name;
        }
    }

    interface LayerPathSetter {

        /**
         * Set the path for the layer.
         *
         * @param pending the pending layer
         * @param root    the root
         * @return {@code true} if the root wasn't set, {@code false} otherwise
         */
        boolean setPath(LayerPathConfig pending, File root);

    }

    /**
     * Resolve the installed layers and add-ons.
     *
     * @param image the installed image
     * @return the installed layers
     */
    static InstalledConfiguration createInstalledConfig(final InstalledImage image) {
        final InstalledConfiguration conf = new InstalledConfiguration(image);
        // Process layers
        final File layersDir = conf.getLayersMetadataDir();
        final File[] layers = layersDir.listFiles();
        if (layers != null) {
            for (final File layer : layers) {
                // Only valid with version information
                final File identity = new File(layer, INSTALLATION_METADATA);
                if (identity.isFile()) {
                    conf.getInstalledLayers().add(layer.getName());
                }
            }
        }
        // Process add-ons
        final File addOnsDir = conf.getAddOnsMetadataDir();
        final File[] addOns = addOnsDir.listFiles();
        if (layers != null) {
            for (final File addOn : addOns) {
                // Only valid with version information
                final File identity = new File(addOn, INSTALLATION_METADATA);
                if (identity.isFile()) {
                    conf.getInstalledAddOns().add(addOn.getName());
                }
            }
        }
        return conf;
    }

    static class ProcessedLayers {

        private final InstalledConfiguration conf;
        private final LayerPathConfig base = new LayerPathConfig(BASE);
        private final Map<String, LayerPathConfig> layers = new LinkedHashMap<String, LayerPathConfig>();
        private final Map<String, LayerPathConfig> addOns = new LinkedHashMap<String, LayerPathConfig>();

        ProcessedLayers(InstalledConfiguration conf) {
            this.conf = conf;
        }

        LayerPathConfig getBase() {
            return base;
        }

        InstalledConfiguration getConf() {
            return conf;
        }

        Map<String, LayerPathConfig> getLayers() {
            return layers;
        }

        Map<String, LayerPathConfig> getAddOns() {
            return addOns;
        }

        void addLayer(final String name, final File root, final LayerPathSetter setter) {
            if (!conf.getInstalledLayers().contains(name)) {
                throw processingError("layer '%s' not configured ", name);
            }
            LayerPathConfig pending = layers.get(name);
            if (pending == null) {
                pending = new LayerPathConfig(name);
                layers.put(name, pending);
            }
            if (!setter.setPath(pending, root)) {
                // Already set means duplicate
                throw processingError("duplicate layer " + name);
            }
        }

        void addAddOn(final String name, final File root, LayerPathSetter setter) {
            if (!conf.getInstalledAddOns().contains(name)) {
                throw processingError("add-on '%s' not configured ", name);
            }
            LayerPathConfig pending = addOns.get(name);
            if (pending == null) {
                pending = new LayerPathConfig(name);
                addOns.put(name, pending);
            }
            if (!setter.setPath(pending, root)) {
                // Already set means duplicate
                throw processingError("duplicate add-on " + name);
            }
        }

    }

    static class InstalledConfiguration {

        final File metadata;
        final InstalledImage installedImage;
        final Set<String> installedLayers = new HashSet<String>();
        final Set<String> installedAddOns = new HashSet<String>();

        InstalledConfiguration(final InstalledImage installedImage) {
            this.metadata = installedImage.getInstallationMetadata();
            this.installedImage = installedImage;
        }

        File getBaseMetadataDir() {
            return new File(metadata, BASE);
        }

        File getLayersMetadataDir() {
            return new File(metadata, LAYERS);
        }

        File getLayerMetadataDir(final String name) {
            return new File(getLayersMetadataDir(), name);
        }

        File getAddOnsMetadataDir() {
            return new File(metadata, ADD_ONS);
        }

        File getAddOnMetadataDir(final String name) {
            return new File(getAddOnsMetadataDir(), name);
        }

        Set<String> getInstalledLayers() {
            return installedLayers;
        }

        Set<String> getInstalledAddOns() {
            return installedAddOns;
        }
    }

    /**
     * Process the layers.conf file.
     *
     * @param repoRoot the repository root
     * @return the layers conf
     * @throws IOException
     */
    static LayersConfig getLayersConfig(final File repoRoot) throws IOException {
        File layersList = new File(repoRoot, LAYERS_CONF);
        if (!layersList.exists()) {
            return new LayersConfig();
        }
        final Properties properties = loadProperties(layersList);
        return new LayersConfig(properties);
    }

    static Properties loadProperties(final File file) throws IOException {
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            final Properties props = new Properties();
            props.load(reader);
            return props;
        } finally {
            safeClose(reader);
        }
    }

    static class LayersConfig {

        private final boolean configured;
        private final String layersPath;
        private final String addOnsPath;
        private final List<String> layers;

        private LayersConfig() {
            configured = false;
            layersPath = DEFAULT_LAYERS_PATH;
            addOnsPath = DEFAULT_ADD_ONS_PATH;
            layers = Collections.emptyList();
        }

        private LayersConfig(Properties properties) {
            configured = true;
            // Possible future enhancement; probably better to use an xml file
            layersPath = properties.getProperty("layers.path", DEFAULT_LAYERS_PATH);
            addOnsPath = properties.getProperty("add-ons.path", DEFAULT_ADD_ONS_PATH);
            String layersProp = (String) properties.get("layers");
            if (layersProp == null || (layersProp = layersProp.trim()).length() == 0) {
                layers = Collections.emptyList();
            } else {
                String[] layerNames = layersProp.split(",");
                layers = new ArrayList<String>();
                for (String layerName : layerNames) {
                    layers.add(layerName);
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

    static IllegalStateException processingError(final String message, final Object... params) {
        return new IllegalStateException(String.format(message, params));
    }

}
