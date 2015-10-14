/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jipijapa.eclipselink;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.persistence.jpa.Archive;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

/**
 * This is the guts of the Eclipse-to-JBossAS7 integration for
 * automatic entity class discovery. The entry point is
 * JBossArchiveFactoryImpl; see there for how to use this.
 *
 * @author Rich DiCroce
 *
 */
public class VFSArchive implements Archive {

    protected URL rootUrl;
    protected String descriptorLocation;
    protected VirtualFile root;
    protected Map<String, VirtualFile> files;

    public VFSArchive(URL rootUrl, String descriptorLocation) throws URISyntaxException, IOException {
        this.rootUrl = rootUrl;
        this.descriptorLocation = descriptorLocation;

        root = VFS.getChild(rootUrl.toURI());
        List<VirtualFile> children = root.getChildrenRecursively(new VirtualFileFilter() {
            @Override
            public boolean accepts(VirtualFile file) {
                return file.isFile();
            }
        });

        files = new HashMap<String, VirtualFile>();
        for (VirtualFile file : children) {
            files.put(file.getPathNameRelativeTo(root), file);
        }
    }

    @Override
    public Iterator<String> getEntries() {
        return files.keySet().iterator();
    }

    @Override
    public InputStream getEntry(String entryPath) throws IOException {
        return files.containsKey(entryPath) ? files.get(entryPath).openStream() : null;
    }

    @Override
    public URL getEntryAsURL(String entryPath) throws IOException {
        return files.containsKey(entryPath) ? files.get(entryPath).asFileURL() : null;
    }

    @Override
    public URL getRootURL() {
        return rootUrl;
    }

    @Override
    public InputStream getDescriptorStream() throws IOException {
        return files.get(descriptorLocation).openStream();
    }

    @Override
    public void close() {} // nothing to close

}
