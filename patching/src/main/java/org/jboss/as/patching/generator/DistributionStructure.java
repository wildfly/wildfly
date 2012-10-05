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

    String getBundleName(DistributionContentItem bundleRoot);

    String getBundleSlot(DistributionContentItem bundleRoot);

    String getModuleName(DistributionContentItem moduleRoot);

    String getModuleSlot(DistributionContentItem moduleRoot);

    /** {@link DistributionStructure} that corresponds to the structure of the AS7 series */
    static class AS7InstallationStructure implements DistributionStructure {

        private static final DistributionContentItem ROOT = DistributionContentItem.createDistributionRoot();
        private static final DistributionContentItem MODULES = new DistributionContentItem("modules", DistributionContentItem.Type.MODULE_PARENT, ROOT, true);
        private static final DistributionContentItem BUNDLES = new DistributionContentItem("bundles", DistributionContentItem.Type.BUNDLE_PARENT, ROOT, true);
        private static final DistributionContentItem STANDALONE = new DistributionContentItem("standalone", DistributionContentItem.Type.MISC, ROOT, true);
        private static final DistributionContentItem DOMAIN = new DistributionContentItem("domain", DistributionContentItem.Type.MISC, ROOT, true);

        @Override
        public DistributionContentItem getCurrentVersionPath(DistributionContentItem previousVersionItem, DistributionStructure previousVersionStructure) {
            return previousVersionItem;
        }

        @Override
        public DistributionContentItem getPreviousVersionPath(DistributionContentItem currentVersionItem, DistributionStructure previousVersionStructure) {
            return currentVersionItem;
        }

        @Override
        public DistributionContentItem getContentItem(File file, DistributionContentItem parent) {
            DistributionContentItem.Type pathType;
            String name = file.getName();
            boolean directory = file.isDirectory();
            switch (parent.getType()) {
                case DISTRIBUTION_ROOT:
                    if ("bundles".equals(name)) {
                        pathType = DistributionContentItem.Type.BUNDLE_PARENT;
                    } else if ("modules".equals(name)) {
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
                    if (STANDALONE.equals(parent) || DOMAIN.equals(parent)) {
                        pathType = "configuration".equals(name) ? DistributionContentItem.Type.IGNORED : DistributionContentItem.Type.MISC;
                    } else {
                        pathType = DistributionContentItem.Type.MISC;
                    }
                    break;
                case IGNORED:
                default:
                    throw new IllegalArgumentException();
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
    }
}
