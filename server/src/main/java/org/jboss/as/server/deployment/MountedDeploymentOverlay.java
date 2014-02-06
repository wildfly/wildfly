package org.jboss.as.server.deployment;

import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.xnio.IoUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * @author Stuart Douglas
 */
public class MountedDeploymentOverlay implements Closeable {

    private Closeable closeable;
    private final File realFile;
    private final VirtualFile mountPoint;
    private final TempFileProvider tempFileProvider;

    public MountedDeploymentOverlay(Closeable closeable, File realFile, VirtualFile mountPoint, TempFileProvider tempFileProvider) {
        this.closeable = closeable;
        this.realFile = realFile;
        this.mountPoint = mountPoint;
        this.tempFileProvider = tempFileProvider;
    }

    public void remountAsZip(boolean expanded) throws IOException {
        IoUtils.safeClose(closeable);
        if(expanded) {
            closeable = VFS.mountZipExpanded(realFile, mountPoint, tempFileProvider);
        } else {
            closeable = VFS.mountZip(realFile, mountPoint, tempFileProvider);
        }
    }

    @Override
    public void close() throws IOException {
        closeable.close();
    }
}
