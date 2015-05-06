/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
