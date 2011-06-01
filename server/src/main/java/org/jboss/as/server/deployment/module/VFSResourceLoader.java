/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.module;

import java.security.CodeSigner;
import java.security.CodeSource;
import org.jboss.modules.ClassSpec;
import org.jboss.modules.PackageSpec;
import org.jboss.modules.PathUtils;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.modules.Resource;
import org.jboss.modules.ResourceLoader;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.FilterVirtualFileVisitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Resource loader capable of loading resources from VFS archives.
 *
 * @author John Bailey
 */
public class VFSResourceLoader implements ResourceLoader {

    private final VirtualFile root;
    private final String rootName;
    private final Manifest manifest;
    private final URL rootUrl;

    /**
     * Construct new instance.
     *
     * @param rootName The module root name
     * @param root The root virtual file
     * @throws IOException if the manifest could not be read or the root URL is invalid
     */
    public VFSResourceLoader(final String rootName, final VirtualFile root) throws IOException {
        this.root = root;
        this.rootName = rootName;
        manifest = VFSUtils.getManifest(root);
        rootUrl = root.asFileURL();
    }

    /** {@inheritDoc} */
    public ClassSpec getClassSpec(final String name) throws IOException {
        final VirtualFile file = root.getChild(name);
        if (!file.exists()) {
            return null;
        }
        final long size = file.getSize();
        final ClassSpec spec = new ClassSpec();
        final InputStream is = file.openStream();
        try {
            if (size <= (long) Integer.MAX_VALUE) {
                final int castSize = (int) size;
                byte[] bytes = new byte[castSize];
                int a = 0, res;
                while ((res = is.read(bytes, a, castSize - a)) > 0) {
                    a += res;
                }
                // done
                is.close();
                spec.setBytes(bytes);
                spec.setCodeSource(new CodeSource(rootUrl, file.getCodeSigners()));
                return spec;
            } else {
                throw new IOException("Resource is too large to be a valid class file");
            }
        } finally {
            VFSUtils.safeClose(is);
        }
    }

    /** {@inheritDoc} */
    public PackageSpec getPackageSpec(final String name) throws IOException {
        final PackageSpec spec = new PackageSpec();
        final Manifest manifest = this.manifest;
        if (manifest == null) {
            return spec;
        }
        final Attributes mainAttribute = manifest.getAttributes(name);
        final Attributes entryAttribute = manifest.getAttributes(name);
        spec.setSpecTitle(getDefinedAttribute(Attributes.Name.SPECIFICATION_TITLE, entryAttribute, mainAttribute));
        spec.setSpecVersion(getDefinedAttribute(Attributes.Name.SPECIFICATION_VERSION, entryAttribute, mainAttribute));
        spec.setSpecVendor(getDefinedAttribute(Attributes.Name.SPECIFICATION_VENDOR, entryAttribute, mainAttribute));
        spec.setImplTitle(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_TITLE, entryAttribute, mainAttribute));
        spec.setImplVersion(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_VERSION, entryAttribute, mainAttribute));
        spec.setImplVendor(getDefinedAttribute(Attributes.Name.IMPLEMENTATION_VENDOR, entryAttribute, mainAttribute));
        if (Boolean.parseBoolean(getDefinedAttribute(Attributes.Name.SEALED, entryAttribute, mainAttribute))) {
            spec.setSealBase(rootUrl);
        }
        return spec;
    }

    private static String getDefinedAttribute(Attributes.Name name, Attributes entryAttribute, Attributes mainAttribute) {
        final String value = entryAttribute == null ? null : entryAttribute.getValue(name);
        return value == null ? mainAttribute == null ? null : mainAttribute.getValue(name) : value;
    }

    /** {@inheritDoc} */
    public String getLibrary(final String name) {
        return null;
    }

    /** {@inheritDoc} */
    public String getRootName() {
        return rootName;
    }

    /** {@inheritDoc} */
    public PathFilter getExportFilter() {
        return PathFilters.acceptAll();
    }

    /** {@inheritDoc} */
    public Resource getResource(final String name) {
        try {
            final VirtualFile file = root.getChild(PathUtils.canonicalize(name));
            if (!file.exists()) {
                return null;
            }
            return new VFSEntryResource(file, file.toURL());
        } catch (MalformedURLException e) {
            // must be invalid...?  (todo: check this out)
            return null;
        }
    }

    /** {@inheritDoc} */
    public Collection<String> getPaths() {
        final List<String> index = new ArrayList<String>();
        // First check for an index file
        final VirtualFile indexFile = VFS.getChild(root.getPathName() + ".index");
        if (indexFile.exists()) {
            try {
                final BufferedReader r = new BufferedReader(new InputStreamReader(indexFile.openStream()));
                try {
                    String s;
                    while ((s = r.readLine()) != null) {
                        index.add(s.trim());
                    }
                    return index;
                } finally {
                    // if exception is thrown, undo index creation
                    r.close();
                }
            } catch (IOException e) {
                index.clear();
            }
        }

        FilterVirtualFileVisitor visitor = new FilterVirtualFileVisitor(new VirtualFileFilter() {
            @Override
            public boolean accepts(VirtualFile file) {
                return file.isDirectory();
            }
        }, VisitorAttributes.RECURSE);
        try {
            root.visit(visitor);
        } catch (IOException e) {
            index.clear();
        }

        index.add("");
        for (VirtualFile dir : visitor.getMatched()) {
            index.add(dir.getPathNameRelativeTo(root));
        }

        return index;
    }

    static class VFSEntryResource implements Resource {
        private final VirtualFile entry;
        private final URL resourceURL;

        VFSEntryResource(final VirtualFile entry, final URL resourceURL) {
            this.entry = entry;
            this.resourceURL = resourceURL;
        }

        public String getName() {
            return entry.getName();
        }

        public URL getURL() {
            return resourceURL;
        }

        public InputStream openStream() throws IOException {
            return entry.openStream();
        }

        public long getSize() {
            final long size = entry.getSize();
            return size == -1 ? 0 : size;
        }
    }

}
