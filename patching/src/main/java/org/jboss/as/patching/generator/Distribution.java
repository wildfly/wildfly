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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.patching.IoUtils;

/**
 * A processed distribution, which maintains a tree of misc files and layers with module and bundles. A persisted version
 * of this structure can be used as comparison base for further processing.
 *
 * @author Emanuel Muckenhuber
 */
class Distribution {

    // The distribution root
    // Node to self: this cannot be static, because of the associated children
    protected final DistributionContentItem ROOT = new DistributionItemImpl(null, null, IoUtils.NO_CONTENT, IoUtils.NO_CONTENT, false);

    private final DistributionStructure structure;
    private final Map<String, ProcessedLayer> layers = new LinkedHashMap<String, ProcessedLayer>();
    private final Map<String, ProcessedLayer> addOns = new LinkedHashMap<String, ProcessedLayer>();

    private String name;
    private String version;

    /**
     * Create and process the distribution right away.
     *
     * @param file the distribution root
     * @return the processed distribution
     * @throws IOException
     */
    public static Distribution create(final File file) throws IOException {
        final Distribution distribution = new Distribution();
        DistributionProcessor.process(distribution.ROOT, file, distribution);
        return distribution;
    }

    Distribution() {
        this.structure = new DistributionStructureImpl(ROOT);
    }

    /**
     * Get the misc file tree.
     *
     * @return the misc root
     */
    DistributionContentItem getRoot() {
        return ROOT;
    }

    /**
     * Get the distribution name.
     *
     * @return the distribution name
     */
    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    /**
     * Get the distribution version.
     *
     * @return the version
     */
    String getVersion() {
        return version;
    }

    void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get the available layer names.
     *
     * @return the layer names
     */
    Set<String> getLayers() {
        return Collections.unmodifiableSet(layers.keySet());
    }

    /**
     * Get a layer.
     * <p/>
     * param name the layer name
     *
     * @return the layer, {@code null} if not available
     */
    ProcessedLayer getLayer(final String name) {
        return layers.get(name);
    }

    /**
     * Get the available add-on names.
     *
     * @return the add-on names
     */
    Set<String> getAddOns() {
        return Collections.unmodifiableSet(addOns.keySet());
    }

    /**
     * Get an add-on.
     *
     * @param name the add-on name
     * @return the add-on, {@code null} if not available
     */
    ProcessedLayer getAddOn(final String name) {
        return addOns.get(name);
    }

    public boolean isIgnored(final DistributionContentItem item) {
        return structure.isIgnored(item);
    }

    public boolean isModuleLookupPath(final DistributionContentItem item) {
        return structure.isModuleLookupPath(item);
    }

    public boolean isBundleLookupPath(final DistributionContentItem item) {
        return structure.isBundleLookupPath(item);
    }

    /**
     * Add a layer.
     *
     * @param name the layer name
     * @return the layer
     */
    protected ProcessedLayer addLayer(final String name) {
        return getOrCreate(name, layers);
    }

    /**
     * Add an add-on.
     *
     * @param name the add-on name
     * @return the add-on
     */
    protected ProcessedLayer addAddOn(final String name) {
        return getOrCreate(name, addOns);
    }

    private static ProcessedLayer getOrCreate(final String layerName, final Map<String, ProcessedLayer> layers) {
        ProcessedLayer layer = layers.get(layerName);
        if (layer == null) {
            layer = new ProcessedLayer(layerName);
            layers.put(layerName, layer);
        }
        return layer;
    }

    static class ProcessedLayer {

        private final String name;
        ProcessedLayer(String name) {
            this.name = name;
        }

        private final Set<DistributionModuleItem> bundles = new TreeSet<DistributionModuleItem>();
        private final Set<DistributionModuleItem> modules = new TreeSet<DistributionModuleItem>();

        String getName() {
            return name;
        }

        /**
         * Get the available bundles.
         *
         * @return the bundles
         */
        Set<DistributionModuleItem> getBundles() {
            return bundles;
        }

        /**
         * Get the available modules.
         *
         * @return the modules
         */
        Set<DistributionModuleItem> getModules() {
            return modules;
        }

        /**
         * Add a bundle.
         *
         * @param item the content item
         */
        protected void addBundle(DistributionContentItem item) {
            bundles.add(createDistributionModuleItem(item));
        }

        /**
         * Add a module.
         *
         * @param item the content item
         */
        protected void addModule(final DistributionContentItem item) {
            modules.add(createDistributionModuleItem(item));
        }

        /**
         * Transform a content item to a module item.
         *
         * @param item the content item
         * @return the module item
         */
        protected DistributionModuleItem createDistributionModuleItem(final DistributionContentItem item) {
            final String moduleName = item.getParent().getPath('.');
            final String slot = item.getName();
            final byte[] metadata = item.getComparisonHash();
            final byte[] comparison = item.getComparisonHash(); // TODO calculate module hash !!
            return new DistributionModuleItem(moduleName, slot, comparison, metadata);
        }

    }

}
