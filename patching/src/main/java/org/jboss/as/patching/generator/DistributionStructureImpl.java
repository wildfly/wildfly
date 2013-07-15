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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.as.patching.IoUtils;

/**
 * @author Brian Stansberry
 * @author Emanuel Muckenhuber
 */
class DistributionStructureImpl implements DistributionStructure {

    private static final String PATH_DELIMITER = "/";
    private final DistributionContentItem ROOT;
    private final DistributionContentItem MODULES;
    private final DistributionContentItem BUNDLES;

    private final Set<DistributionContentItem> moduleSearchPath = new TreeSet<DistributionContentItem>();
    private final Set<DistributionContentItem> bundleSearchPath = new TreeSet<DistributionContentItem>();
    private final List<DistributionContentItem.Filter> ignored = new ArrayList<DistributionContentItem.Filter>();

    protected DistributionStructureImpl(final DistributionContentItem root) {

        ROOT = root;
        MODULES = createMiscItem(root, "modules");
        BUNDLES = createMiscItem(root, "bundles");
        moduleSearchPath.add(MODULES);
        bundleSearchPath.add(BUNDLES);

        // Ignore the identity owned files, they should never be changed by diff
        registerIgnoredPath("bin/product.conf");
        registerIgnoredPath("modules/layers.conf");
        registerIgnoredPath("bundles/layers.conf");

        // Ignore configuration and runtime locations
        registerIgnoredPath("appclient/configuration**");
        registerIgnoredPath("appclient/data**");
        registerIgnoredPath("appclient/log**");
        registerIgnoredPath("appclient/tmp**");
        registerIgnoredPath("domain/configuration**");
        registerIgnoredPath("domain/data**");
        registerIgnoredPath("domain/log**");
        registerIgnoredPath("domain/servers**");
        registerIgnoredPath("domain/tmp**");
        registerIgnoredPath("standalone/configuration**");
        registerIgnoredPath("standalone/data**");
        registerIgnoredPath("standalone/log**");
        registerIgnoredPath("standalone/tmp**");
    }

    @Override
    public void registerStandardModuleSearchPath(String name, String standardPath) {
        moduleSearchPath.add(createMiscItem(ROOT, standardPath));
    }

    @Override
    public void excludeDefaultModuleRoot() {
        moduleSearchPath.remove(MODULES);
    }

    @Override
    public void registerStandardBundleSearchPath(String name, String standardPath) {
        bundleSearchPath.add(createMiscItem(ROOT, standardPath));
    }

    @Override
    public void excludeDefaultBundleRoot() {
        bundleSearchPath.remove(BUNDLES);
    }

    @Override
    public void registerIgnoredPath(String path) {
        final DistributionContentItem.Filter filter = new DistributionContentItem.GlobPathFilter(path);
        ignored.add(filter);
    }

    @Override
    public boolean isModuleLookupPath(DistributionContentItem item) {
        return moduleSearchPath.contains(item);
    }

    @Override
    public boolean isBundleLookupPath(DistributionContentItem item) {
        return bundleSearchPath.contains(item);
    }

    @Override
    public boolean isIgnored(final DistributionContentItem item) {
        for (final DistributionContentItem.Filter filter : ignored) {
            if (filter.accept(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCompatibleWith(DistributionStructure other) {
        return (getClass().equals(other.getClass()));
    }

    static DistributionContentItem createMiscItem(final DistributionContentItem parent, final String path) {
        DistributionContentItem result = parent;
        final String[] s = path.split(PATH_DELIMITER);
        final int length = s.length;
        for (int i = 0; i < length; i++) {
            boolean dir = i < length - 1;
            result = new DistributionItemImpl(result, s[i], IoUtils.NO_CONTENT, IoUtils.NO_CONTENT, !dir);
        }
        return result;
    }
}
