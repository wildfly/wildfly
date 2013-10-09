package org.wildfly.extension.undertow.deployment;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceManager;
import org.jboss.vfs.VirtualFile;


/**
 * @author Stuart Douglas
 */
public class VirtualFileResourceManager implements ResourceManager {

    private volatile VirtualFile base;

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

    public long getTransferMinSize() {
        return transferMinSize;
    }
}
