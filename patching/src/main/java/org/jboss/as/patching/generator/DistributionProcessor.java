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

import static org.jboss.as.patching.generator.PatchGenerator.processingError;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.patching.installation.LayersConfig;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * Utility class for processing a distribution.
 *
 * @author Emanuel Muckenhuber
 */
class DistributionProcessor {

    private Set<DistributionContentItem> moduleRoots = new LinkedHashSet<DistributionContentItem>();

    /**
     * Process a distribution root.
     *
     * @param parent           the misc root
     * @param distributionRoot the distribution root
     * @param distribution     the distribution
     * @throws IOException
     */
    static void process(final DistributionContentItem parent, final File distributionRoot, Distribution distribution) throws IOException {
        final DistributionProcessor processor = new DistributionProcessor();
        final File[] children = distributionRoot.listFiles();
        if (children != null && children.length != 0) {
            for (final File child : children) {
                processor.processMisc(parent, child, distribution);
            }
        }

        final List<File> mp = new ArrayList<File>();
        final Set<DistributionContentItem> moduleRoots = processor.moduleRoots;
        for (final DistributionContentItem item : moduleRoots) {
            final File file = item.getFile(distributionRoot);
            mp.add(file);
        }

        // Update name and version
        final ModuleLoader loader = new LocalModuleLoader(mp.toArray(new File[mp.size()]));
        try {
            Module module = loader.loadModule(ModuleIdentifier.create("org.jboss.as.version"));

            final Class<?> clazz = module.getClassLoader().loadClass("org.jboss.as.version.ProductConfig");
            final Method resolveName = clazz.getMethod("resolveName");
            final Method resolveVersion  = clazz.getMethod("resolveVersion");
            final Constructor<?> constructor = clazz.getConstructor(ModuleLoader.class, String.class, Map.class);

            final Object productConfig = constructor.newInstance(loader, distributionRoot.getAbsolutePath(), Collections.emptyMap());

            distribution.setName((String) resolveName.invoke(productConfig));
            distribution.setVersion((String) resolveVersion.invoke(productConfig));

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Process the misc files.
     *
     * @param parent       the parent content item
     * @param root         the current root
     * @param distribution the distribution
     * @throws IOException
     */
    void processMisc(final DistributionContentItem parent, final File root, final Distribution distribution) throws IOException {
        final DistributionContentItem item = new DistributionItemFileImpl(root, parent);
        if (distribution.isIgnored(item)) {
            // Skip ignored ... Maybe only files?
            return;
        } else if (distribution.isModuleLookupPath(item)) {
            // Process modules
            final LayeredContext lc = new LayeredModuleContext(distribution);
            processLayeredRoot(item, root, lc);
            return;
        } else if (distribution.isBundleLookupPath(item)) {
            /// Process bundles
            final LayeredContext lc = new LayeredBundleContext(distribution);
            processLayeredRoot(item, root, lc);
            return;
        }

        // Build the misc file tree
        parent.getChildren().add(item);
        // Process the children
        final File[] children = root.listFiles();
        if (children != null && children.length != 0) {
            for (final File child : children) {
                processMisc(item, child, distribution);
            }
        }
    }

    /**
     * Process the layered root. This maybe should move to {@code DistributionStructure}.
     *
     * @param parent  the parent content item
     * @param root    the current root
     * @param context the layered context (bundle/module)
     * @throws IOException
     */
    void processLayeredRoot(final DistributionContentItem parent, final File root, final LayeredContext context) throws IOException {
        final LayersConfig layersConfig = LayersConfig.getLayersConfig(root);
        // Process layers
        final File layersDir = new File(root, layersConfig.getLayersPath());
        final DistributionContentItem newParent = DistributionStructureImpl.createMiscItem(parent, layersConfig.getLayersPath());
        if (!layersDir.exists()) {
            if (layersConfig.isConfigured()) {
                // Bad config from user
                throw processingError("No layers directory found at " + layersDir);
            }
            // else this isn't a root that has layers and add-ons
            return;
        }
        // check for a valid layer configuration
        final List<String> layers;
        if (layersConfig.isConfigured()) {
            layers = layersConfig.getLayers();
            if (layers.size() != layersDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathName) {
                    return pathName.isDirectory();
                }
            }).length) {
                throw processingError("configured layers does not match actual ones %s", layers);
            }
        } else {
            // At least we need to have base, right?
            layers = Collections.singletonList("base");
        }
        //
        for (final String layer : layers) {
            File layerDir = new File(layersDir, layer);
            if (!layerDir.exists()) {
                if (layersConfig.isConfigured()) {
                    // Bad config from user
                    throw processingError("Cannot find layer '%s' under directory %s", layer, layersDir);
                }
                // else this isn't a standard layers and add-ons structure
                return;
            }
            context.addLayer(newParent, layer, layerDir);
        }
        // Finally process the add-ons
        final File addOnsDir = new File(root, layersConfig.getAddOnsPath());
        final File[] addOnsList = addOnsDir.listFiles();
        if (addOnsList != null) {
            for (final File addOn : addOnsList) {
                context.addAddOn(newParent, addOn.getName(), addOn);
            }
        }
    }

    /**
     * Try to find determine the modules.
     *
     * @param parent  the parent content item
     * @param root    the current root
     * @param context the module context
     */
    void processModules(final DistributionContentItem parent, final File root, final ModuleContext context) {

        final DistributionContentItem item = new DistributionItemFileImpl(root, parent);
        final File moduleXml = new File(root, "module.xml");
        if (moduleXml.exists()) {
            // Only ignore actual modules
            if (context.isIgnored(item)) {
                return;
            }
            context.addModule(item);
        }
        final File[] children = root.listFiles();
        if (children != null && children.length != 0) {
            for (final File child : children) {
                processModules(item, child, context);
            }
        }
    }

    /**
     * Try to determine the bundles.
     *
     * @param parent  the parent content item
     * @param root    the current root
     * @param context the bundle context
     */
    void processBundles(final DistributionContentItem parent, final File root, final ModuleContext context) {

        final DistributionContentItem item = new DistributionItemFileImpl(root, parent);
        final File[] children = root.listFiles();
        if (children != null && children.length != 0) {
            for (final File child : children) {
                if (!child.isDirectory()) {
                    // Only ignore actual bundles
                    if (context.isIgnored(item)) {
                        return;
                    }
                    context.addModule(item);
                    return;
                }
            }
            for (final File child : children) {
                processBundles(item, child, context);
            }
        }
    }

    interface ProcessorContext {

        boolean isIgnored(final DistributionContentItem item);

    }

    interface ModuleContext extends ProcessorContext {

        void addModule(final DistributionContentItem module);
    }

    abstract static class LayeredContext implements ProcessorContext {

        protected final Distribution distribution;

        protected LayeredContext(Distribution distribution) {
            this.distribution = distribution;
        }

        @Override
        public boolean isIgnored(DistributionContentItem item) {
            return distribution.isIgnored(item);
        }

        void addLayer(DistributionContentItem parent, String layer, File layerDir) {
            final Distribution.ProcessedLayer processedLayer = distribution.addLayer(layer);
            final DistributionContentItem item = new DistributionItemFileImpl(layerDir, parent);
            addModuleRoot(item);
            doProcess(layerDir, processedLayer);
        }

        void addAddOn(DistributionContentItem parent, String name, File addOn) {
            final Distribution.ProcessedLayer processedLayer = distribution.addAddOn(name);
            final DistributionContentItem item = new DistributionItemFileImpl(addOn, parent);
            addModuleRoot(item);
            doProcess(addOn, processedLayer);
        }

        void addModuleRoot(final DistributionContentItem item) {
            //
        }

        void doProcess(final File layerDir, final Distribution.ProcessedLayer processedLayer) {
            final File[] children = layerDir.listFiles();
            if (children != null && children.length > 0) {
                for (final File child : children) {
                    // Skip the layer dir as parent... we only need the module name and layer
                    process(null, child, processedLayer);
                }
            }
        }

        /**
         * Process either a bundle or module.
         *
         * @param parent         the parent content item
         * @param layerDir       the layer dir
         * @param processedLayer the currently processed layer
         */
        abstract void process(DistributionContentItem parent, File layerDir, Distribution.ProcessedLayer processedLayer);

    }

    class LayeredModuleContext extends LayeredContext {

        LayeredModuleContext(Distribution distribution) {
            super(distribution);
        }

        @Override
        void addModuleRoot(DistributionContentItem item) {
            moduleRoots.add(item);
        }

        void process(DistributionContentItem parent, File layerDir, final Distribution.ProcessedLayer processedLayer) {
            processModules(parent, layerDir, new ModuleContext() {
                @Override
                public void addModule(DistributionContentItem module) {
                    processedLayer.addModule(module);
                }

                @Override
                public boolean isIgnored(DistributionContentItem item) {
                    return distribution.isIgnored(item);
                }
            });
        }
    }

    class LayeredBundleContext extends LayeredContext {

        LayeredBundleContext(Distribution distribution) {
            super(distribution);
        }

        @Override
        void process(final DistributionContentItem parent, final File layerDir, final Distribution.ProcessedLayer processedLayer) {
            processBundles(parent, layerDir, new ModuleContext() {
                @Override
                public void addModule(DistributionContentItem module) {
                    processedLayer.addBundle(module);
                }

                @Override
                public boolean isIgnored(DistributionContentItem item) {
                    return distribution.isIgnored(item);
                }
            });
        }
    }

}
