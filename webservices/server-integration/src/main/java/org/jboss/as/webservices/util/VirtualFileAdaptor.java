/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.util;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.vfs.VirtualFile;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;

/**
 * A VirtualFile adaptor.
 *
 * @author Thomas.Diesler@jboss.org
 * @author Ales.Justin@jboss.org
 * @author alessio.soldano@jboss.com
 */
public final class VirtualFileAdaptor implements UnifiedVirtualFile {

    private static final long serialVersionUID = -4509594124653184349L;

    private final transient VirtualFile file;

    public VirtualFileAdaptor(VirtualFile file) {
        this.file = file;
    }

    private VirtualFile getFile() throws IOException {
        return file;
    }

    private UnifiedVirtualFile findChild(String child, boolean throwExceptionIfNotFound) throws IOException {
        final VirtualFile virtualFile = getFile();
        final VirtualFile childFile = file.getChild(child);
        if (!childFile.exists()) {
            if (throwExceptionIfNotFound) {
                throw WSLogger.ROOT_LOGGER.missingChild(child, virtualFile);
            } else {
                WSLogger.ROOT_LOGGER.tracef("Child '%s' not found for VirtualFile: %s", child, virtualFile);
                return null;
            }
        }
        return new VirtualFileAdaptor(childFile);
    }

    public UnifiedVirtualFile findChild(String child) throws IOException {
        return findChild(child, true);
    }

    public UnifiedVirtualFile findChildFailSafe(String child) {
        try {
            return findChild(child, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public URL toURL() {
        try {
            return getFile().toURL();
        } catch (Exception e) {
            return null;
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
