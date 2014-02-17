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

package org.jboss.as.patching.tool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.patching.Constants;
import org.jboss.as.patching.PatchInfo;
import org.jboss.as.patching.PatchMessages;
import org.jboss.as.patching.PatchingException;
import org.jboss.as.patching.installation.InstallationManager;
import org.jboss.as.patching.installation.LayersConfig;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.runner.PatchToolImpl;
import org.jboss.as.patching.runner.PatchUtils;
import org.jboss.as.version.ProductConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.LocalModuleLoader;
import org.jboss.modules.ModuleLoader;

/**
 * The patch tool.
 *
 * @author Emanuel Muckenhuber
 */
public interface PatchTool {

    ContentVerificationPolicy DEFAULT = ContentVerificationPolicy.STRICT;

    /**
     * Get the patch info.
     *
     * @return the patch info
     */
    PatchInfo getPatchInfo();

    /**
     * Get patching history
     *
     * @return  patching history
     */
    PatchingHistory getPatchingHistory();

    /**
     * Apply a patch.
     *
     * @param file the patch file
     * @param contentPolicy the content verification policy
     * @return the patching result
     * @throws PatchingException
     */
    PatchingResult applyPatch(File file, ContentVerificationPolicy contentPolicy) throws PatchingException;

    /**
     * Apply a patch.
     *
     * @param url the url to retrieve the patch from
     * @param contentPolicy the content verification policy
     * @return the patching result
     * @throws PatchingException
     */
    PatchingResult applyPatch(URL url, ContentVerificationPolicy contentPolicy) throws PatchingException;

    /**
     * Apply a patch.
     *
     * @param is the content input stream
     * @param contentPolicy the content verification policy
     * @return the patching result
     * @throws PatchingException
     */
    PatchingResult applyPatch(InputStream is, ContentVerificationPolicy contentPolicy) throws PatchingException;

    /**
     * Rollback a patch.
     *
     * @param patchId the patch id
     * @param contentPolicy the content verification policy
     * @param rollbackTo rollback all one off patches until the given patch-id
     * @param resetConfiguration whether to reset the configuration from the backup
     * @return the patching result
     * @throws PatchingException
     */
    PatchingResult rollback(String patchId, ContentVerificationPolicy contentPolicy, boolean rollbackTo, boolean resetConfiguration) throws PatchingException;

    /**
     * Rollback the last applied patch.
     *
     * @param contentPolicy the content verification policy
     * @param resetConfiguration whether to reset the configuration from the backup
     * @return the patching result
     * @throws PatchingException
     */
    PatchingResult rollbackLast(ContentVerificationPolicy contentPolicy, boolean resetConfiguration) throws PatchingException;

    public class Factory {

        private Factory() {
            //
        }

        /**
         * Create a new policy builder instance.
         *
         * @return a content policy builder
         */
        public static ContentPolicyBuilder policyBuilder() {
            return new ContentPolicyBuilderImpl();
        }

        /**
         * Create a content verification policy from a dmr model
         *
         * @param operation the model node
         * @return the policy
         */
        public static ContentVerificationPolicy create(final ModelNode operation) {
            final PatchTool.ContentPolicyBuilder builder = policyBuilder();
            final boolean overrideModules = operation.get(Constants.OVERRIDE_MODULES).asBoolean(false);
            if(overrideModules) {
                builder.ignoreModuleChanges();
            }
            final boolean overrideAll = operation.get(Constants.OVERRIDE_ALL).asBoolean(false);
            if(overrideAll) {
                builder.overrideAll();
            }
            if(operation.hasDefined(Constants.OVERRIDE)) {
                final ModelNode overrides = operation.get(Constants.OVERRIDE);
                for(final ModelNode override : overrides.asList()) {
                    builder.overrideItem(override.asString());
                }
            }
            if(operation.hasDefined(Constants.PRESERVE)) {
                final ModelNode preserves = operation.get(Constants.PRESERVE);
                for(final ModelNode preserve : preserves.asList()) {
                    builder.preserveItem(preserve.asString());
                }
            }
            return builder.createPolicy();
        }

        /**
         * Create an offline local patch tool.
         *
         * @param jbossHome   the distribution root
         * @param moduleRoots the module roots
         * @param bundleRoots the bundle roots
         * @return the patch tool
         * @throws IOException
         */
        public static PatchTool createLocalTool(final File jbossHome, final List<File> moduleRoots, final List<File> bundleRoots) throws IOException {
            final File[] resolvedPath = resolveLayeredModulePath(moduleRoots); // Resolve the patched module root for the module loader
            final ModuleLoader loader = new LocalModuleLoader(resolvedPath);
            final ProductConfig config = new ProductConfig(loader, jbossHome.getAbsolutePath(), Collections.emptyMap());
            final InstallationManager manager = InstallationManager.load(jbossHome, moduleRoots, bundleRoots, config);
            return create(manager);
        }

        /**
         * Create an offline local patch tool.
         *
         * @param manager the installation manager
         * @return the patch tool
         */
        public static PatchTool create(final InstallationManager manager) {
            return new PatchToolImpl(manager);
        }

        private static File[] resolveLayeredModulePath(List<File> modulePath) {

            boolean foundLayers = false;
            List<File> layeredPath = new ArrayList<File>();
            for (File file : modulePath) {

                // Always add the root, as the user may place modules directly in it
                layeredPath.add(file);

                LayersConfig layersConfig = getLayersConfig(file);

                File layersDir = new File(file, layersConfig.getLayersPath());
                if (!layersDir.exists())  {
                    if (layersConfig.isConfigured()) {
                        // Bad config from user
                        throw PatchMessages.MESSAGES.installationNoLayersConfigFound(layersDir.getAbsolutePath());
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
                            throw PatchMessages.MESSAGES.installationMissingLayer(layerName, layersDir.getAbsolutePath());
                        }
                        // else this isn't a standard layers and add-ons structure
                        validLayers = false;
                        break;
                    }
                    loadOverlays(layer, layerFiles);
                }
                if (validLayers) {
                    foundLayers = true;
                    layeredPath.addAll(layerFiles);
                    // Now add-ons
                    File[] addOns = new File(file, layersConfig.getAddOnsPath()).listFiles();
                    if (addOns != null) {
                        for (File addOn : addOns) {
                            if (addOn.isDirectory()) {
                                loadOverlays(addOn, layeredPath);
                            }
                        }
                    }
                }
            }

            return foundLayers ? layeredPath.toArray(new File[layeredPath.size()]) : modulePath.toArray(new File[modulePath.size()]);
        }

        private static LayersConfig getLayersConfig(File repoRoot) {
            try {
                return LayersConfig.getLayersConfig(repoRoot);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        static void loadOverlays(final File layeringRoot, final List<File> path) {
            final File overlays = new File(layeringRoot, Constants.OVERLAYS);
            if (overlays.exists()) {
                final File refs = new File(overlays, Constants.OVERLAYS);
                if (refs.exists()) {
                    try {
                        for (final String overlay : PatchUtils.readRefs(refs)) {
                            final File root = new File(overlays, overlay);
                            path.add(root);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            path.add(layeringRoot);
        }

    }

    public interface ContentPolicyBuilder {

        /**
         * Build the resulting policy.
         *
         * @return the content verification policy
         */
        ContentVerificationPolicy createPolicy();

        /**
         * Ignore all local module changes.
         *
         * @return the builder
         */
        ContentPolicyBuilder ignoreModuleChanges();

        /**
         * Override all local changes.
         *
         * @return the builder
         */
        ContentPolicyBuilder overrideAll();

        /**
         * Override a misc content item.
         *
         * @param item the item to override
         * @return the builder
         */
        ContentPolicyBuilder overrideItem(MiscContentItem item);

        /**
         * Override a misc content item.
         *
         * @param path the path of the item
         * @return the builder
         */
        ContentPolicyBuilder overrideItem(String path);

        /**
         * Preserve an existing content item.
         *
         * @param item the item to preserve
         * @return the builder
         */
        ContentPolicyBuilder preserveItem(MiscContentItem item);

        /**
         * Preserve an existing content item.
         *
         * @param path the path of the item
         * @return the builder
         */
        ContentPolicyBuilder preserveItem(String path);

    }

}
