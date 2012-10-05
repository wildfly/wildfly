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

package org.jboss.as.patching.runner;

import org.jboss.as.patching.metadata.BundleItem;
import org.jboss.as.patching.metadata.ContentItem;
import org.jboss.as.patching.metadata.ContentType;
import org.jboss.as.patching.metadata.MiscContentItem;
import org.jboss.as.patching.metadata.ModuleItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Emanuel Muckenhuber
 */
class PatchContentLoader {

    public static final String MODULES = "modules";
    public static final String BUNDLES = "modules";
    public static final String MISC = "misc";

    private final File miscRoot;
    private final File bundlesRoot;
    private final File modulesRoot;

    static PatchContentLoader create(final File root) {
        final File miscRoot = new File(root, PatchContentLoader.MISC);
        final File bundlesRoot = new File(root, PatchContentLoader.BUNDLES);
        final File modulesRoot = new File(root, PatchContentLoader.MODULES);
        return new PatchContentLoader(miscRoot, bundlesRoot, modulesRoot);
    }

    PatchContentLoader(final File miscRoot, final File bundlesRoot, final File modulesRoot) {
        this.bundlesRoot  = bundlesRoot;
        this.miscRoot = miscRoot;
        this.modulesRoot = modulesRoot;
    }

    /**
     * Open a new content stream.
     *
     * @param item the content item
     * @return the content stream
     */
    InputStream openContentStream(final ContentItem item) throws IOException {
        final File file = getFile(item);
        return new FileInputStream(file);
    }

    /**
     * Get a patch content file.
     *
     * @param item the content item
     * @return the file
     * @throws IOException
     */
    File getFile(final ContentItem item) throws IOException {
        final ContentType content = item.getContentType();
        switch (content) {
            case MODULE:
                return getModulePath((ModuleItem) item);
            case MISC:
                return getMiscPath((MiscContentItem) item);
            case BUNDLE:
                return getBundlePath((BundleItem) item);
            default:
                throw new IllegalStateException();
        }
    }

    File getMiscPath(final MiscContentItem item) {
        return getMiscPath(miscRoot, item);
    }

    File getModulePath(final ModuleItem item) {
        return getModulePath(modulesRoot, item);
    }

    File getBundlePath(final BundleItem item) {
        return getModulePath(bundlesRoot, item.getName(), item.getSlot());
    }

    static File getMiscPath(final File miscRoot, final MiscContentItem item) {
        File file = miscRoot;
        for(final String path : item.getPath()) {
            file = new File(file, path);
        }
        file = new File(file, item.getName());
        return file;
    }

    static File getModulePath(File root, ModuleItem item) {
        return getModulePath(root, item.getName(), item.getSlot());
    }

    static File getModulePath(File root, String name, String slot) {
        final String[] ss = name.split("\\.");
        File file = root;
        for(final String s : ss) {
            file = new File(file, s);
        }
        return new File(file, slot);
    }

}
