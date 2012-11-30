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

package org.jboss.as.patching.generator;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates rules about the structure of a given standard unzipped distribution of the application server.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public interface DistributionStructure {

    /** Factory for a {@link DistributionStructure */
    public static class Factory {
        /**
         * Create a {@link DistributionStructure} for the given version.
         *
         * @param version the
         */
        public static DistributionStructure create(String version) {
            return new AS7InstallationStructure();
        }
    }

    /**
     * Value object representing a search path for a module or a bundle.
     */
    public static final class SlottedContentSearchPath {
        private final String name;
        private final DistributionContentItem path;

        public SlottedContentSearchPath(String name, DistributionContentItem path) {
            this.name = name;
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public DistributionContentItem getPath() {
            return path;
        }
    }

    /**
     * Register a module search path.
     * @param name logical name for the path
     * @param standardPath the path
     */
    void registerStandardModuleSearchPath(String name, String standardPath);

    /**
     * Configures this structure to no include the default module search path (modules/) as a valid path.
     */
    void excludeDefaultModuleRoot();

    /**
     * Register a bundle search path.
     * @param name logical name for the path
     * @param standardPath the path
     */
    void registerStandardBundleSearchPath(String name, String standardPath);

    /**
     * Configures this structure to no include the default bundle search path (bundles/) as a valid path.
     */
    void excludeDefaultBundleRoot();

    /**
     * Register a path that should be ignored when analyzing the structure for differences.
     *
     * @param path the path
     */
    void registerIgnoredPath(String path);

    /**
     * Get a content item representing the root directory for a module
     * @param moduleName the name of the module
     * @param slot the module slot
     * @param searchPath the logical name of a registered module search path, or {@code null} or the empty string
     *                   to use the default search path
     * @return the content item
     *
     * @throws IllegalArgumentException if {@code searchPath} is unknown
     */
    DistributionContentItem getModuleRootContentItem(String moduleName, String slot, String searchPath);

    /**
     * Get a content item representing the root directory for a bundle
     * @param bundleName the name of the module
     * @param slot the module slot
     * @param searchPath the logical name of a registered bundle search path, or {@code null} or the empty string
     *                   to use the default search path
     * @return the content item
     *
     * @throws IllegalArgumentException if {@code searchPath} is unknown
     */
    DistributionContentItem getBundleRootContentItem(String bundleName, String slot, String searchPath);

    /**
     * Get a content item representing miscellaneous content
     * @param path the path of the content
     * @param directory {@code true} if the content is a directory
     *
     * @return the content item
     */
    DistributionContentItem getMiscContentItem(String path, boolean directory);

    /**
     * Gets whether this {@code DistributionStructure} is compatible with another distribution structure. Compatible
     * structures will, immediately after construction, have the same default bundle and module paths, the same
     * default ignored paths, and the same rules for determining the types of content items.
     *
     * @param other the other structure
     *
     * @return {@code true} if the structures are compatible, {@code false} otherwise
     */
    boolean isCompatibleWith(DistributionStructure other);

    /**
     * Create a {@link DistributionContentItem content item} for the given {@code file}, which is a child of a file
     * represented by the given {@code parent} content item
     *
     * @param file the file. Cannot be {@code null}
     * @param parent the parent content item. Cannot be {@code null}
     * @return the content item
     */
    DistributionContentItem getContentItem(File file, DistributionContentItem parent);

    /**
     * Gets a {@link DistributionContentItem content item} for the distribution structure represented by this
     * object that corresponds to the given {@code previousVersionItem} that was associated with an earlier version's
     * distribution structure.
     * <p>
     * Use this method to translate the location of an item from an old version's distribution structure to the
     * logically equivalent location in this structure.
     * </p>
     *
     * @param previousVersionItem the item from the previous version
     * @param previousVersionStructure the structure of the previous version
     * @return the corresponding item from this structure
     */
    DistributionContentItem getCurrentVersionPath(DistributionContentItem previousVersionItem, DistributionStructure previousVersionStructure);

    /**
     * Gets a {@link DistributionContentItem content item} for the distribution structure represented by the
     * given previous version's structure that corresponds to the given {@code currentVersionItem} that is associated
     * with this version's distribution structure.
     * <p>
     * Use this method to translate the location of an item from the current version's structure to the
     * logically equivalent location in a previous version's structure.
     * </p>
     *
     * @param currentVersionItem the item from the current version
     * @param previousVersionStructure the structure of the previous version
     * @return the corresponding item from the previous structure
     */
    DistributionContentItem getPreviousVersionPath(DistributionContentItem currentVersionItem, DistributionStructure previousVersionStructure);

    /**
     * Gets the search path under which the given {@code bundleRoot} can be found
     * @param bundleRoot a {@link DistributionContentItem} of type {@link DistributionContentItem.Type#BUNDLE_ROOT}
     *
     * @return the search path
     *
     * @throws IllegalArgumentException if {@code bundleRoot} is not located under any known search path
     */
    SlottedContentSearchPath getBundleSearchPath(DistributionContentItem bundleRoot);

    /**
     * Gets the name of the bundle represented by the given {@code bundleRoot} content item
     * @param bundleRoot a {@link DistributionContentItem} of type {@link DistributionContentItem.Type#BUNDLE_ROOT}
     *
     * @return the bundle name
     */
    String getBundleName(DistributionContentItem bundleRoot);

    /**
     * Gets the slot of the bundle represented by the given {@code bundleRoot} content item
     * @param bundleRoot a {@link DistributionContentItem} of type {@link DistributionContentItem.Type#BUNDLE_ROOT}
     *
     * @return the bundle slot
     */
    String getBundleSlot(DistributionContentItem bundleRoot);

    /**
     * Gets the search path under which the given {@code moduleRoot} can be found
     * @param moduleRoot a {@link DistributionContentItem} of type {@link DistributionContentItem.Type#MODULE_ROOT}
     *
     * @return the search path
     *
     * @throws IllegalArgumentException if {@code moduleRoot} is not located under any known search path
     */
    SlottedContentSearchPath getModuleSearchPath(DistributionContentItem moduleRoot);

    /**
     * Gets the name of the module represented by the given {@code moduleRoot} content item
     * @param moduleRoot a {@link DistributionContentItem} of type {@link DistributionContentItem.Type#MODULE_ROOT}
     *
     * @return the module name
     */
    String getModuleName(DistributionContentItem moduleRoot);

    /**
     * Gets the slot of the module represented by the given {@code moduleRoot} content item
     * @param moduleRoot a {@link DistributionContentItem} of type {@link DistributionContentItem.Type#MODULE_ROOT}
     *
     * @return the module slot
     */
    String getModuleSlot(DistributionContentItem moduleRoot);

    /** {@link DistributionStructure} that corresponds to the structure of the AS7 series */
    static class AS7InstallationStructure implements DistributionStructure {

        private static final DistributionContentItem ROOT = DistributionContentItem.createDistributionRoot();
        private static final DistributionContentItem MODULES = new DistributionContentItem("modules", DistributionContentItem.Type.MODULE_PARENT, ROOT, true);
        private static final DistributionContentItem BUNDLES = new DistributionContentItem("bundles", DistributionContentItem.Type.BUNDLE_PARENT, ROOT, true);

        private final Map<DistributionContentItem, String> moduleSearchPathsByPath = new HashMap<DistributionContentItem, String>();
        private final Map<String, DistributionContentItem> moduleSearchPathsByName = new HashMap<String, DistributionContentItem>();
        private final Map<DistributionContentItem, String> bundleSearchPathsByPath = new HashMap<DistributionContentItem, String>();
        private final Map<String, DistributionContentItem> bundleSearchPathsByName = new HashMap<String, DistributionContentItem>();
        private final Set<DistributionContentItem> ignoredPaths = new HashSet<DistributionContentItem>();

        protected AS7InstallationStructure() {
            registerStandardModuleSearchPath("", MODULES.getPath());
            registerStandardBundleSearchPath("", BUNDLES.getPath());
            registerIgnoredPath("standalone/configuration");
            registerIgnoredPath("standalone/data");
            registerIgnoredPath("standalone/log");
            registerIgnoredPath("standalone/tmp");
            registerIgnoredPath("domain/configuration");
            registerIgnoredPath("domain/data");
            registerIgnoredPath("domain/log");
            registerIgnoredPath("domain/servers");
            registerIgnoredPath("domain/tmp");
        }

        @Override
        public void registerStandardModuleSearchPath(String name, String standardPath) {
            DistributionContentItem item = createSlottedContentBase(standardPath, false);
            String existing = moduleSearchPathsByPath.put(item, name);
            moduleSearchPathsByName.put(name, item);
            if (existing != null) {
                moduleSearchPathsByName.remove(existing);
            }
        }

        @Override
        public void registerStandardBundleSearchPath(String name, String standardPath) {
            DistributionContentItem item = createSlottedContentBase(standardPath, true);
            String existing = bundleSearchPathsByPath.put(item, name);
            bundleSearchPathsByName.put(name, item);
            if (existing != null) {
                bundleSearchPathsByName.remove(existing);
            }
        }

        @Override
        public void registerIgnoredPath(String path) {
            ignoredPaths.add(DistributionContentItem.createMiscItemForPath(path));
        }

        @Override
        public void excludeDefaultModuleRoot() {
            DistributionContentItem item = moduleSearchPathsByName.remove("");
            if (item != null) {
                moduleSearchPathsByPath.remove(item);
            }
        }

        @Override
        public void excludeDefaultBundleRoot() {
            DistributionContentItem item = bundleSearchPathsByName.remove("");
            if (item != null) {
                bundleSearchPathsByPath.remove(BUNDLES);
            }
        }

        @Override
        public DistributionContentItem getCurrentVersionPath(DistributionContentItem previousVersionItem, DistributionStructure previousVersionStructure) {
            return previousVersionItem;
        }

        @Override
        public DistributionContentItem getPreviousVersionPath(DistributionContentItem currentVersionItem, DistributionStructure previousVersionStructure) {
            return currentVersionItem;
        }

        @Override
        public SlottedContentSearchPath getBundleSearchPath(DistributionContentItem bundleRoot) {
            DistributionContentItem item = bundleRoot;
            while (item != null) {
                String name = bundleSearchPathsByPath.get(item);
                if (name != null) {
                    return new SlottedContentSearchPath(name, item);
                }
                item = item.getParent();
            }
            throw new IllegalArgumentException("No known bundle root");
        }

        @Override
        public DistributionContentItem getModuleRootContentItem(String moduleName, String slot, String searchPath) {
            String search = searchPath == null ? "" : searchPath;
            DistributionContentItem item = moduleSearchPathsByName.get(search);
            if (item == null) {
                throw new IllegalArgumentException("Unknown search-path " + searchPath);
            }
            for (String name : splitSlottedContentName(moduleName)) {
                item = new DistributionContentItem(name, DistributionContentItem.Type.MODULE_PARENT, item, true);
            }
            final String slotName = slot == null ? "main" : slot;
            return new DistributionContentItem(slotName, DistributionContentItem.Type.MODULE_ROOT, item, true);
        }

        @Override
        public DistributionContentItem getBundleRootContentItem(String bundleName, String slot, String searchPath) {
            String search = searchPath == null ? "" : searchPath;
            DistributionContentItem item = bundleSearchPathsByName.get(search);
            if (item == null) {
                throw new IllegalArgumentException("Unknown search-path " + searchPath);
            }
            for (String name : splitSlottedContentName(bundleName)) {
                item = new DistributionContentItem(name, DistributionContentItem.Type.BUNDLE_PARENT, item, true);
            }
            final String slotName = slot == null ? "main" : slot;
            return new DistributionContentItem(slotName, DistributionContentItem.Type.BUNDLE_ROOT, item, true);
        }

        @Override
        public DistributionContentItem getMiscContentItem(String path, boolean directory) {
            DistributionContentItem item = ROOT;
            String[] split = splitPath(path);
            for (int i = 0; i < split.length - 1; i++) {
                item = new DistributionContentItem(split[i], DistributionContentItem.Type.MISC, item, true);
            }
            if (split.length > 0) {
                item = new DistributionContentItem(split[split.length - 1], DistributionContentItem.Type.MISC, item, directory);
            }
            return item;
        }

        @Override
        public boolean isCompatibleWith(DistributionStructure other) {
            return (getClass().equals(other.getClass()));
        }

        @Override
        public DistributionContentItem getContentItem(File file, DistributionContentItem parent) {
            DistributionContentItem.Type pathType;
            String name = file.getName();
            boolean directory = file.isDirectory();
            DistributionContentItem test = new DistributionContentItem(name, DistributionContentItem.Type.MISC, parent, directory);
            if (ignoredPaths.contains(test)) {
                pathType = DistributionContentItem.Type.IGNORED;
            } else {
                switch (parent.getType()) {
                    case DISTRIBUTION_ROOT:
                        if (bundleSearchPathsByPath.containsKey(test)) {
                            pathType = DistributionContentItem.Type.BUNDLE_PARENT;
                        } else if (moduleSearchPathsByPath.containsKey(test)) {
                            pathType = DistributionContentItem.Type.MODULE_PARENT;
                        } else {
                            pathType = DistributionContentItem.Type.MISC;
                        }
                        break;
                    case BUNDLE_PARENT:
                        // determine if this file has any non-directory children. If so, it is a bundle root
                        pathType = DistributionContentItem.Type.BUNDLE_PARENT;
                        File[] bundleChildren = file.listFiles();
                        if (bundleChildren != null) {
                            for (File child : bundleChildren) {
                                if (!child.isDirectory()) {
                                    pathType = DistributionContentItem.Type.BUNDLE_ROOT;
                                    break;
                                }
                            }
                        } // else cruft -- perhaps log a WARN?
                        break;
                    case BUNDLE_ROOT:
                        pathType = DistributionContentItem.Type.BUNDLE_CONTENT;
                        break;
                    case BUNDLE_CONTENT:
                        pathType = DistributionContentItem.Type.BUNDLE_CONTENT;
                        break;
                    case MODULE_PARENT:
                        String[] moduleChildren = directory ? file.list() : null;
                        if (moduleChildren == null || moduleChildren.length == 0) {
                            // Cruft-- perhaps log a WARN?
                            pathType = DistributionContentItem.Type.IGNORED;
                        } else {
                            // If the file has a module.xml child, it's a module root
                            pathType = DistributionContentItem.Type.MODULE_PARENT;
                            for (String child : moduleChildren) {
                                if ("module.xml".equals(child)) {
                                    pathType = DistributionContentItem.Type.MODULE_ROOT;
                                }
                            }
                        }
                        break;
                    case MODULE_ROOT:
                        pathType = DistributionContentItem.Type.MODULE_CONTENT;
                        break;
                    case MODULE_CONTENT:
                        pathType = DistributionContentItem.Type.MODULE_CONTENT;
                        break;
                    case MISC:
                        if (bundleSearchPathsByPath.containsKey(test)) {
                            pathType = DistributionContentItem.Type.BUNDLE_PARENT;
                        } else if (moduleSearchPathsByPath.containsKey(test)) {
                            pathType = DistributionContentItem.Type.MODULE_PARENT;
                        } else {
                            pathType = DistributionContentItem.Type.MISC;
                        }
                        break;
                    case IGNORED:
                    default:
                        throw new IllegalArgumentException();
                }
            }
            return new DistributionContentItem(name, pathType, parent, directory);
        }

        @Override
        public String getBundleName(DistributionContentItem bundleRoot) {
            assert bundleRoot.getType() == DistributionContentItem.Type.BUNDLE_ROOT : "Invalid type " + bundleRoot.getType();
            DistributionContentItem moduleNamePath = bundleRoot.getRelativeItem(BUNDLES).getParent();
            return moduleNamePath.getPath('.');
        }

        @Override
        public String getBundleSlot(DistributionContentItem bundleRoot) {
            assert bundleRoot.getType() == DistributionContentItem.Type.BUNDLE_ROOT : "Invalid type " + bundleRoot.getType();
            return bundleRoot.getName();
        }

        @Override
        public SlottedContentSearchPath getModuleSearchPath(DistributionContentItem moduleRoot) {
            DistributionContentItem item = moduleRoot;
            while (item != null) {
                String name = moduleSearchPathsByPath.get(item);
                if (name != null) {
                    return new SlottedContentSearchPath(name, item);
                }
                item = item.getParent();
            }
            throw new IllegalArgumentException("No known bundle root");
        }

        @Override
        public String getModuleName(DistributionContentItem moduleRoot) {
            assert moduleRoot.getType() == DistributionContentItem.Type.MODULE_ROOT : "Invalid type " + moduleRoot.getType();
            DistributionContentItem moduleNamePath = moduleRoot.getRelativeItem(MODULES).getParent();
            return moduleNamePath.getPath('.');
        }

        @Override
        public String getModuleSlot(DistributionContentItem moduleRoot) {
            assert moduleRoot.getType() == DistributionContentItem.Type.MODULE_ROOT : "Invalid type " + moduleRoot.getType();
            return moduleRoot.getName();
        }

        private static String[] splitSlottedContentName(String name) {
            return name.split("\\.");
        }

        private static String[] splitPath(String path) {
            return path.split("/");
        }

        static DistributionContentItem createSlottedContentBase(String path, boolean bundle) {
            DistributionContentItem.Type finalType = bundle ? DistributionContentItem.Type.BUNDLE_PARENT : DistributionContentItem.Type.MODULE_PARENT;
            DistributionContentItem result = DistributionContentItem.createDistributionRoot();
            final String[] s = path.split(DistributionContentItem.PATH_DELIMITER);
            final int length = s.length;
            for (int i = 0; i < length; i++) {
                boolean last = i < length - 1;
                result = new DistributionContentItem(s[i], last ? finalType : DistributionContentItem.Type.MISC, result, last);
            }
            return result;
        }
    }
}
