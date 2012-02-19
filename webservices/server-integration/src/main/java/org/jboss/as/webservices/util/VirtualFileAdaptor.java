/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.webservices.util;

import static org.jboss.as.webservices.WSMessages.MESSAGES;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.deployment.WritableUnifiedVirtualFile;
import org.jboss.wsf.spi.util.URLLoaderAdapter;

/**
 * A VirtualFile adaptor.
 *
 * @author Thomas.Diesler@jboss.org
 * @author Ales.Justin@jboss.org
 * @author alessio.soldano@jboss.com
 */
public final class VirtualFileAdaptor implements WritableUnifiedVirtualFile {
    private static final long serialVersionUID = -4509594124653184348L;

    private static final ObjectStreamField[] serialPersistentFields = { new ObjectStreamField("rootUrl", URL.class),
            new ObjectStreamField("path", String.class), new ObjectStreamField("requiresMount", boolean.class) };

    /** Minimal info to get full vfs file structure */
    private URL rootUrl;
    private String path;
    private boolean requiresMount;
    /** The virtual file */
    private transient VirtualFile file;

    public VirtualFileAdaptor(VirtualFile file) {
        this.file = file;
    }

    public VirtualFileAdaptor(URL rootUrl, String path) {
        this(rootUrl, path, false);
    }

    protected VirtualFileAdaptor(URL rootUrl, String path, boolean requiresMount) {
        if (rootUrl == null)
            throw MESSAGES.nullRootUrl();
        if (path == null)
            throw MESSAGES.nullPath();

        this.rootUrl = rootUrl;
        this.path = path;
        this.requiresMount = requiresMount;
    }

    /**
     * Get the virtual file. Create file from root url and path if it doesn't exist yet.
     *
     * @return virtual file root
     * @throws IOException for any error
     */
    protected VirtualFile getFile() throws IOException {
        if (file == null) {
            VirtualFile root;
            try {
                root = VFS.getChild(rootUrl.toURI());
            } catch (URISyntaxException e) {
                throw MESSAGES.cannotGetVirtualFile(e, rootUrl);
            }
            file = root.getChild(path);

            if (!file.exists()) {
                throw MESSAGES.missingVirtualFile(file);
            } else if (requiresMount && !isMounted(root, file)) {
                throw MESSAGES.unmountedVirtualFile(file);
            }
        }
        return file;
    }

    private static boolean isMounted(VirtualFile root, VirtualFile child) throws IOException {
        return !(root.getPathName().equals(root.getPhysicalFile().getAbsolutePath()) && child.getPathName().equals(
                child.getPhysicalFile().getAbsolutePath()));
    }

    public UnifiedVirtualFile findChild(String child) throws IOException {
        final VirtualFile virtualFile = getFile();
        final VirtualFile childFile = file.getChild(child);
        if (!childFile.exists())
            throw MESSAGES.missingChild(child, virtualFile);
        return new VirtualFileAdaptor(childFile);
    }

    public URL toURL() {
        try {
            return getFile().toURL();
        } catch (Exception e) {
            return null;
        }
    }

    public void writeContent(OutputStream bos) throws IOException {
        writeContent(bos, null);
    }

    public void writeContent(OutputStream bos, NameFilter filter) throws IOException {
        InputStream is = null;
        try {
            is = getFile().openStream();
            if (is instanceof JarInputStream) {
                JarInputStream jis = (JarInputStream) is;
                JarOutputStream os = new JarOutputStream(bos);
                JarEntry je = null;
                while ((je = jis.getNextJarEntry()) != null) {
                    if (filter != null && filter.accept(je.getName())) {
                        os.putNextEntry(je);
                        VFSUtils.copyStream(jis, os);
                    }
                }
                VFSUtils.safeClose(os);
            } else {
                VFSUtils.copyStream(is, bos);
            }
        } finally {
            VFSUtils.safeClose(is);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException, URISyntaxException {
        VirtualFile file = getFile();
        URL url = rootUrl;
        if (url == null) {
            VirtualFile parentFile = file.getParent();
            url = parentFile != null ? parentFile.toURL() : null;
        }
        String pathName = path;
        if (pathName == null)
            pathName = file.getName();

        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("rootUrl", url);
        fields.put("path", pathName);

        URI uri = url != null ? url.toURI() : null;
        VirtualFile newRoot = VFS.getChild(uri);
        VirtualFile newChild = newRoot.getChild(pathName);
        fields.put("requiresMount", isMounted(newRoot, newChild));

        out.writeFields();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = in.readFields();
        rootUrl = (URL) fields.get("rootUrl", null);
        path = (String) fields.get("path", null);
        requiresMount = fields.get("requiresMount", false);
    }

    private Object writeReplace() {
        // TODO: hack to enable remote tests
        try {
            File archive = file.getPhysicalFile();
            if (archive.list().length == 0) {
                final File parent = file.getPhysicalFile().getParentFile();
                final File[] children = parent.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File fileOrDir) {
                        return fileOrDir.isFile();
                    }
                });
                archive = children[0];
            }
            // Offer different UnifiedVirtualFile implementation for deserialization process
            return new URLLoaderAdapter(archive.toURI().toURL());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<UnifiedVirtualFile> getChildren() throws IOException {
        List<VirtualFile> vfList = getFile().getChildren();
        if (vfList == null)
            return null;
        List<UnifiedVirtualFile> uvfList = new LinkedList<UnifiedVirtualFile>();
        for (VirtualFile vf : vfList) {
            uvfList.add(new VirtualFileAdaptor(vf));
        }
        return uvfList;
    }

    public String getName() {
        try {
            return getFile().getName();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
