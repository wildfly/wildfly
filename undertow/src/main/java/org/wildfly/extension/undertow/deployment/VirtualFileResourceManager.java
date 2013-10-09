package org.wildfly.extension.undertow.deployment;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeEvent;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import org.jboss.vfs.VirtualFile;
import org.xnio.FileChangeCallback;
import org.xnio.FileChangeEvent;
import org.xnio.FileSystemWatcher;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Xnio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @author Stuart Douglas
 */
public class VirtualFileResourceManager implements ResourceManager {

    private volatile VirtualFile base;
    private final List<ResourceChangeListener> listeners = new ArrayList<ResourceChangeListener>();
    private FileSystemWatcher fileSystemWatcher;

    /**
     * Size to use direct FS to network transfer (if supported by OS/JDK) instead of read/write
     */
    private final long transferMinSize;

    public VirtualFileResourceManager(final VirtualFile base, long transferMinSize) {
        if (base == null) {
            throw UndertowMessages.MESSAGES.argumentCannotBeNull("base");
        }
        this.base = base;
        this.transferMinSize = transferMinSize;
    }

    public VirtualFile getBase() {
        return base;
    }

    public VirtualFileResourceManager setBase(final VirtualFile base) {
        if (base == null) {
            throw UndertowMessages.MESSAGES.argumentCannotBeNull("base");
        }
        this.base = base;
        return this;
    }

    public Resource getResource(final String p) {
        String path = p;
        if (p.startsWith("/")) {
            path = p.substring(1);
        }
        try {
            VirtualFile file = base.getChild(p);
            if (file.exists()) {
                return new VirtualFileResource(file.getPhysicalFile(), this, path);
            } else {
                return null;
            }
        } catch (Exception e) {
            UndertowLogger.REQUEST_LOGGER.debugf(e, "Invalid path %s");
            return null;
        }
    }

    @Override
    public boolean isResourceChangeListenerSupported() {
        try {
            base.getPhysicalFile();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public synchronized void registerResourceChangeListener(ResourceChangeListener listener) {
        listeners.add(listener);
        if (fileSystemWatcher == null) {
            fileSystemWatcher = Xnio.getInstance().createFileSystemWatcher("Watcher for " + base, OptionMap.EMPTY);
            try {
                final File base = this.base.getPhysicalFile();
                fileSystemWatcher.watchPath(base, new FileChangeCallback() {
                    @Override
                    public void handleChanges(Collection<FileChangeEvent> changes) {
                        synchronized (VirtualFileResourceManager.this) {
                            String basePath = base.getAbsolutePath();
                            final List<ResourceChangeEvent> events = new ArrayList<ResourceChangeEvent>();
                            for (FileChangeEvent change : changes) {
                                if (change.getFile().getAbsolutePath().startsWith(basePath)) {
                                    String path = change.getFile().getAbsolutePath().substring(basePath.length());
                                    events.add(new ResourceChangeEvent(getResource(path), ResourceChangeEvent.Type.valueOf(change.getType().name())));
                                }
                            }
                            for (ResourceChangeListener listener : listeners) {
                                listener.handleChanges(events);
                            }
                        }
                    }
                });
            } catch (IOException e) {

            }
        }
    }


    @Override
    public synchronized void removeResourceChangeListener(ResourceChangeListener listener) {
        listeners.remove(listener);
    }

    public long getTransferMinSize() {
        return transferMinSize;
    }

    @Override
    public void close() throws IOException {
        IoUtils.safeClose(fileSystemWatcher);
    }
}
