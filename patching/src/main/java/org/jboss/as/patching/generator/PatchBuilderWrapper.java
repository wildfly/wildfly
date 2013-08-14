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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.patching.metadata.ModificationBuilderTarget;
import org.jboss.as.patching.metadata.Patch;
import org.jboss.as.patching.metadata.PatchBuilder;
import org.jboss.as.patching.metadata.PatchElementBuilder;

/**
 * Basic wrapper of a {@code PatchBuilder} implementing the comparison of two distributions.
 *
 * @author Emanuel Muckenhuber
 */
abstract class PatchBuilderWrapper extends PatchBuilder {

    protected PatchBuilderWrapper() {
        //
    }

    abstract PatchElementBuilder modifyLayer(final String name, final boolean addOn);

    /**
     * Create a patch based on the comparison of two distributions.
     *
     * @param base    the comparison base
     * @param updated the updated distribution
     * @return the generated patch
     */
    protected Patch compare(Distribution base, Distribution updated) {
        compare(this, base, updated);
        return build();
    }

    /**
     * Callback when the diff determines that a layer was added.
     *
     * @param layer the layer name
     * @return the element builder
     */
    protected PatchElementBuilder addLayer(final String layer) {
        throw processingError("invalid layer configuration for %s", layer);
    }

    /**
     * Callback when there were changes in a layer.
     *
     * @param layer the layer name
     * @return the element builder
     */
    protected PatchElementBuilder modifyLayer(final String layer) {
        return modifyLayer(layer, false);
    }

    /**
     * Callback when the diff determines that a layer was removed.
     *
     * @param layer the layer name
     * @return the element builder
     */
    protected PatchElementBuilder removeLayer(final String layer) {
        throw processingError("invalid layer configuration for %s", layer);
    }

    /**
     * Callback when the diff determines that an add-on was added.
     *
     * @param layer the add-on name
     * @return the element builder
     */
    protected PatchElementBuilder addAddOn(final String layer) {
        throw processingError("invalid add-on configuration for %s", layer);
    }

    /**
     * Callback when there were changes in an add-on.
     *
     * @param layer the layer name
     * @return the element builder
     */
    protected PatchElementBuilder modifyAddOn(final String layer) {
        return modifyLayer(layer, true);
    }

    /**
     * Callback when the diff determines that an add-on was removed.
     *
     * @param layer the add-on name
     * @return the element builder
     */
    protected PatchElementBuilder removeAddOn(final String layer) {
        throw processingError("invalid add-on configuration for %s", layer);
    }

    /**
     * Compare two distributions.
     *
     * @param builder  the patch builder
     * @param original the original distribution
     * @param updated  the updated distribution
     */
    static void compare(final PatchBuilderWrapper builder, final Distribution original, final Distribution updated) {

        // Compare misc files
        final DistributionContentItem or = original.getRoot();
        final DistributionContentItem nr = updated.getRoot();
        compareMiscFiles(builder, or, nr);

        // Compare layers
        final Set<String> originalLayers = new LinkedHashSet<String>(original.getLayers());
        final Set<String> updatedLayers = new LinkedHashSet<String>(updated.getLayers());

        for (final String layer : originalLayers) {
            final Distribution.ProcessedLayer originalLayer = original.getLayer(layer);
            final Distribution.ProcessedLayer updatedLayer;
            final PatchElementBuilder elementBuilder;
            if (updatedLayers.remove(layer)) {
                elementBuilder = builder.modifyLayer(layer);
                updatedLayer = updated.getLayer(layer);
            } else {
                elementBuilder = builder.removeLayer(layer);
                updatedLayer = null;
            }
            //
            compareLayer(elementBuilder, originalLayer, updatedLayer);
        }

        for (final String layer : updatedLayers) {
            final Distribution.ProcessedLayer originalLayer = null;
            final Distribution.ProcessedLayer updatedLayer = updated.getLayer(layer);
            final PatchElementBuilder elementBuilder = builder.addLayer(layer);
            //
            compareLayer(elementBuilder, originalLayer, updatedLayer);
        }

        // Compare add-ons
        final Set<String> originalAddOns = new LinkedHashSet<String>(original.getAddOns());
        final Set<String> updatedAddOns = new LinkedHashSet<String>(updated.getAddOns());

        for (final String addOn : originalAddOns) {
            final Distribution.ProcessedLayer originalLayer = original.getAddOn(addOn);
            final Distribution.ProcessedLayer updatedLayer;
            final PatchElementBuilder elementBuilder;
            if (updatedAddOns.remove(addOn)) {
                elementBuilder = builder.modifyAddOn(addOn);
                updatedLayer = updated.getAddOn(addOn);
            } else {
                elementBuilder = builder.removeAddOn(addOn);
                updatedLayer = null;
            }
            //
            compareLayer(elementBuilder, originalLayer, updatedLayer);
        }

        for (final String addOn : updatedAddOns) {
            final PatchElementBuilder elementBuilder = builder.addAddOn(addOn);
            compareLayer(elementBuilder, null, updated.getAddOn(addOn));
        }

    }

    /**
     * Compare a single layer or add-on.
     *
     * @param elementBuilder the element builder
     * @param originalLayer  the original layer
     * @param updatedLayer   the updated layer
     */
    static void compareLayer(final PatchElementBuilder elementBuilder, final Distribution.ProcessedLayer originalLayer, final Distribution.ProcessedLayer updatedLayer) {
        compareModuleItems(elementBuilder, originalLayer.getModules(), updatedLayer.getModules(), false); // Modules
        compareModuleItems(elementBuilder, originalLayer.getBundles(), updatedLayer.getBundles(), true);  // Bundles
    }

    /**
     * Compare a module or bundle item.
     *
     * @param elementBuilder the element builder
     * @param original       the original module set
     * @param updated        the updated module set
     * @param bundle         whether is a bundle or module
     */
    static void compareModuleItems(final PatchElementBuilder elementBuilder, final Collection<DistributionModuleItem> original,
                                   final Collection<DistributionModuleItem> updated, boolean bundle) {

        final Map<String, DistributionModuleItem> modules = new HashMap<String, DistributionModuleItem>();
        for (final DistributionModuleItem item : updated) {
            modules.put(item.getFullModuleName(), item);
        }

        for (final DistributionModuleItem o : original) {
            final DistributionModuleItem n = modules.remove(o.getFullModuleName());
            if (n == null) {
                if (bundle) {
                    elementBuilder.removeBundle(o.getName(), o.getSlot(), o.getMetadataHash());
                } else {
                    elementBuilder.removeModule(o.getName(), o.getSlot(), o.getMetadataHash());
                }
            } else if (!Arrays.equals(n.getComparisonHash(), o.getComparisonHash())) {
                if (bundle) {
                    elementBuilder.modifyBundle(n.getName(), n.getSlot(), o.getMetadataHash(), n.getMetadataHash());
                } else {
                    elementBuilder.modifyModule(n.getName(), n.getSlot(), o.getMetadataHash(), n.getMetadataHash());
                }
            }
        }
        for (final DistributionModuleItem item : modules.values()) {
            if (bundle) {
                elementBuilder.addBundle(item.getName(), item.getSlot(), item.getMetadataHash());
            } else {
                elementBuilder.addModule(item.getName(), item.getSlot(), item.getMetadataHash());
            }
        }
    }

    /**
     * Compare the misc node tree.
     *
     * @param o the original root
     * @param n the updated root
     */
    static void compareMiscFiles(final ModificationBuilderTarget<?> builder, final DistributionContentItem o, final DistributionContentItem n) {
        if (o == null && n == null) {
            return;
        } else if (o != null && n == null) {
            builder.removeFile(o.getName(), o.getParent().getPathAsList(), o.getMetadataHash(), !o.isLeaf());
        } else if (o == null && n != null) {
            boolean directory = !n.isLeaf();
            if (directory) {
                for (final DistributionContentItem child : n.getChildren()) {
                    compareMiscFiles(builder, null, child);
                }
            } else {
                builder.addFile(n.getName(), n.getParent().getPathAsList(), n.getMetadataHash(), directory);
            }
        } else {
            if (!n.equals(o)) {
                throw processingError("TODO");
            }
            if (n.isLeaf() != o.isLeaf()) {
                throw processingError("TODO");
            }
            if (n.isLeaf() && !Arrays.equals(o.getComparisonHash(), n.getComparisonHash())) {
                builder.modifyFile(n.getName(), n.getParent().getPathAsList(), o.getMetadataHash(), n.getMetadataHash(), !n.isLeaf());
            } else {

                final Collection<DistributionContentItem> nc = n.getChildren();
                final Map<String, DistributionContentItem> children = new HashMap<String, DistributionContentItem>();
                for (final DistributionContentItem child : nc) {
                    children.put(child.getName(), child);
                }
                // compare
                for (final DistributionContentItem child : o.getChildren()) {
                    final DistributionContentItem item = children.remove(child.getName());
                    compareMiscFiles(builder, child, item);
                }
                // compare missing
                for (final DistributionContentItem child : children.values()) {
                    compareMiscFiles(builder, null, child);
                }
            }
        }
    }

}
