package org.jboss.as.capedwarf.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.spi.FileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSigner;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modified file system.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class ModifiedFileSystem implements FileSystem {

    private static AttachmentKey<Map<File, ModifiedFileSystem>> KEY = AttachmentKey.create(Map.class);

    private final File root;
    private final Map<String, File> files;

    static synchronized ModifiedFileSystem get(DeploymentUnit unit, File root) {
        Map<File, ModifiedFileSystem> map = unit.getAttachment(KEY);
        if (map == null) {
            map = new HashMap<File, ModifiedFileSystem>();
            unit.putAttachment(KEY, map);
        }
        ModifiedFileSystem mfs = map.get(root);
        if (mfs == null) {
            mfs = new ModifiedFileSystem(root, Collections.<String, File>emptyMap());
            map.put(root, mfs);
        }
        return mfs;
    }

    static synchronized void remove(DeploymentUnit unit) {
        final Map<File, ModifiedFileSystem> map = unit.removeAttachment(KEY);
        if (map != null) {
            for (ModifiedFileSystem mfs : map.values())
                VFSUtils.safeClose(mfs);
        }
    }

    ModifiedFileSystem(File root, String name, File file) {
        this(root, Collections.singletonMap(name, file));
    }

    ModifiedFileSystem(File root, Map<String, File> files) {
        this.root = root;
        this.files = new ConcurrentHashMap<String, File>(files);
    }

    void addFile(String name, File file) {
        files.put(name, file);
    }

    protected File getFileInternal(VirtualFile mountPoint, VirtualFile target) {
        if (mountPoint.equals(target))
            return root;

        final String path = target.getPathNameRelativeTo(mountPoint);
        final File file = files.get(path);
        if (file != null)
            return file;
        else {
            return new File(root, path);
        }
    }

    public File getFile(VirtualFile mountPoint, VirtualFile target) throws IOException {
        return getFileInternal(mountPoint, target);
    }

    public InputStream openInputStream(VirtualFile mountPoint, VirtualFile target) throws IOException {
        return new FileInputStream(getFileInternal(mountPoint, target));
    }

    public boolean isReadOnly() {
        return true;
    }

    public boolean delete(VirtualFile mountPoint, VirtualFile target) {
        return getFileInternal(mountPoint, target).delete();
    }

    public long getSize(VirtualFile mountPoint, VirtualFile target) {
        return getFileInternal(mountPoint, target).length();
    }

    public long getLastModified(VirtualFile mountPoint, VirtualFile target) {
        return getFileInternal(mountPoint, target).lastModified();
    }

    public boolean exists(VirtualFile mountPoint, VirtualFile target) {
        return getFileInternal(mountPoint, target).exists();
    }

    public boolean isFile(VirtualFile mountPoint, VirtualFile target) {
        return getFileInternal(mountPoint, target).isFile();
    }

    public boolean isDirectory(VirtualFile mountPoint, VirtualFile target) {
        return getFileInternal(mountPoint, target).isDirectory();
    }

    public List<String> getDirectoryEntries(VirtualFile mountPoint, VirtualFile target) {
        final File file = getFileInternal(mountPoint, target);
        final String[] names = file.list();
        return names == null ? Collections.<String>emptyList() : Arrays.asList(names);
    }

    public CodeSigner[] getCodeSigners(VirtualFile mountPoint, VirtualFile target) {
        return null;
    }

    public void close() throws IOException {
    }

    public File getMountSource() {
        return root;
    }

    public URI getRootURI() throws URISyntaxException {
        return root.toURI();
    }
}
