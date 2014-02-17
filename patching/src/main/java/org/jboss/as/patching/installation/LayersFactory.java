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

import static org.jboss.as.patching.Constants.ADD_ONS;
import static org.jboss.as.patching.Constants.LAYERS;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.DirectoryStructure;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.runner.PatchUtils;
import org.jboss.as.version.ProductConfig;

/**
 * @author Emanuel Muckenhuber
 */
class LayersFactory {

    /**
     * Load the available layers.
     *
     * @param image             the installed image
     * @param productConfig     the product config to establish the identity
     * @param moduleRoots       the module roots
     * @param bundleRoots       the bundle roots
     * @return the layers
     * @throws IOException
     */
    static InstalledIdentity load(final InstalledImage image, final ProductConfig productConfig, final List<File> moduleRoots, final List<File> bundleRoots) throws IOException {

        // build the identity information
        final String productVersion = productConfig.resolveVersion();
        final String productName = productConfig.resolveName();
        final Identity identity = new AbstractLazyIdentity() {
            @Override
            public String getName() {
                return productName;
            }

            @Override
            public String getVersion() {
                return productVersion;
            }

            @Override
            public InstalledImage getInstalledImage() {
                return image;
            }
        };

        final Properties properties = PatchUtils.loadProperties(identity.getDirectoryStructure().getInstallationInfo());
        final List<String> allPatches = PatchUtils.readRefs(properties, Constants.ALL_PATCHES);

        // Step 1 - gather the installed layers data
        final InstalledConfiguration conf = createInstalledConfig(image);
        // Step 2 - process the actual module and bundle roots
        final ProcessedLayers processedLayers = process(conf, moduleRoots, bundleRoots);
        final InstalledConfiguration config = processedLayers.getConf();

        // Step 3 - create the actual config objects
        // Process layers
        final InstalledIdentityImpl installedIdentity = new InstalledIdentityImpl(identity, allPatches, image);
        for (final LayerPathConfig layer : processedLayers.getLayers().values()) {
            final String name = layer.name;
            installedIdentity.putLayer(name, createPatchableTarget(name, layer, config.getLayerMetadataDir(name), image));
        }
        // Process add-ons
        for (final LayerPathConfig addOn : processedLayers.getAddOns().values()) {
            final String name = addOn.name;
            installedIdentity.putAddOn(name, createPatchableTarget(name, addOn, config.getAddOnMetadataDir(name), image));
        }
        return installedIdentity;
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
//        if (conf.getInstalledLayers().size() != layers.getLayers().size()) {
//            throw processingError("processed layers don't match expected %s, but was %s", conf.getInstalledLayers(), layers.getLayers().keySet());
//        }
//        if (conf.getInstalledAddOns().size() != layers.getAddOns().size()) {
//            throw processingError("processed add-ons don't match expected %s, but was %s", conf.getInstalledAddOns(), layers.getAddOns().keySet());
//        }
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
        final LayersConfig layersConfig = LayersConfig.getLayersConfig(root);
        // Process layers
        final File layersDir = new File(root, layersConfig.getLayersPath());
        if (!layersDir.exists()) {
            if (layersConfig.isConfigured()) {
                // Bad config from user
                throw PatchMessages.MESSAGES.installationNoLayersConfigFound(layersDir.getAbsolutePath());
            }
            // else this isn't a root that has layers and add-ons
        } else {
            // check for a valid layer configuration
            for (final String layer : layersConfig.getLayers()) {
                File layerDir = new File(layersDir, layer);
                if (!layerDir.exists()) {
                    if (layersConfig.isConfigured()) {
                        // Bad config from user
                        throw PatchMessages.MESSAGES.installationMissingLayer(layer, layersDir.getAbsolutePath());
                    }
                    // else this isn't a standard layers and add-ons structure
                    return;
                }
                layers.addLayer(layer, layerDir, setter);
            }
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
     * Create the actual patchable target.
     *
     * @param name     the layer name
     * @param layer    the layer path config
     * @param metadata the metadata location for this target
     * @param image    the installed image
     * @return the patchable target
     * @throws IOException
     */
    static AbstractLazyPatchableTarget createPatchableTarget(final String name, final LayerPathConfig layer, final File metadata, final InstalledImage image) throws IOException {
        // patchable target
        return new AbstractLazyPatchableTarget() {

            @Override
            public InstalledImage getInstalledImage() {
                return image;
            }

            @Override
            public File getModuleRoot() {
                return layer.modulePath;
            }

            @Override
            public File getBundleRepositoryRoot() {
                return layer.bundlePath;
            }

            public File getPatchesMetadata() {
                return metadata;
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
        // Would be nice to have an installed inventory or smth like that
        return conf;
    }

    static class ProcessedLayers {

        private final InstalledConfiguration conf;
        private final Map<String, LayerPathConfig> layers = new LinkedHashMap<String, LayerPathConfig>();
        private final Map<String, LayerPathConfig> addOns = new LinkedHashMap<String, LayerPathConfig>();

        ProcessedLayers(InstalledConfiguration conf) {
            this.conf = conf;
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
//            if (!conf.getInstalledLayers().contains(name)) {
//                throw processingError("layer '%s' not configured ", name);
//            }
            LayerPathConfig pending = layers.get(name);
            if (pending == null) {
                pending = new LayerPathConfig(name);
                layers.put(name, pending);
            }
            if (!setter.setPath(pending, root)) {
                // Already set means duplicate
                throw PatchMessages.MESSAGES.installationDuplicateLayer("layer", name);
            }
        }

        void addAddOn(final String name, final File root, LayerPathSetter setter) {
//            if (!conf.getInstalledAddOns().contains(name)) {
//                throw processingError("add-on '%s' not configured ", name);
//            }
            LayerPathConfig pending = addOns.get(name);
            if (pending == null) {
                pending = new LayerPathConfig(name);
                addOns.put(name, pending);
            }
            if (!setter.setPath(pending, root)) {
                // Already set means duplicate
                throw PatchMessages.MESSAGES.installationDuplicateLayer("add-on", name);
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
     * @author Emanuel Muckenhuber
     */
    abstract static class AbstractLazyPatchableTarget extends LayerDirectoryStructure implements Layer, AddOn {

        @Override
        File getPatchesMetadata() {
            return getPatchesMetadata(getName());
        }

        @Override
        public DirectoryStructure getDirectoryStructure() {
            return this;
        }

        @Override
        public TargetInfo loadTargetInfo() throws IOException {
            return LayerInfo.loadTargetInfoFromDisk(getDirectoryStructure());
        }

    }

    /**
     * @author Emanuel Muckenhuber
     */
    abstract static class AbstractLazyIdentity extends LayerDirectoryStructure.IdentityDirectoryStructure implements Identity {

        @Override
        public DirectoryStructure getDirectoryStructure() {
            return this;
        }

        @Override
        public TargetInfo loadTargetInfo() throws IOException {
            return LayerInfo.loadTargetInfoFromDisk(getDirectoryStructure());
        }

    }
}
