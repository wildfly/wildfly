/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.deployment;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.vfs.VirtualFile;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.CanonicalPathUtils;

/**
 * Resource manager that deals with overlays
 *
 * @author Stuart Douglas
 */
public class ServletResourceManager implements ResourceManager {

    public static final int TRANSFER_MIN_SIZE = 1024 * 1024;
    private final PathResourceManager deploymentResourceManager;
    private final Collection<VirtualFile> overlays;
    private final ResourceManager[] externalOverlays;
    private final boolean explodedDeployment;

    public ServletResourceManager(final VirtualFile resourcesRoot, final Collection<VirtualFile> overlays,
                                  boolean explodedDeployment, boolean followSymlink, boolean disableFileWatchService,
                                  List<String> externalOverlays) throws IOException {
        this.explodedDeployment = explodedDeployment;
        Path physicalFile = resourcesRoot.getPhysicalFile().toPath().toRealPath();
        deploymentResourceManager = new PathResourceManager(physicalFile, TRANSFER_MIN_SIZE, true,
                followSymlink, !disableFileWatchService);
        this.overlays = overlays;
        if(externalOverlays == null) {
            this.externalOverlays = new ResourceManager[0];
        } else {
            this.externalOverlays = new ResourceManager[externalOverlays.size()];
            for (int i = 0; i < externalOverlays.size(); ++i) {
                String path = externalOverlays.get(i);
                PathResourceManager pr = new PathResourceManager(Paths.get(path).toRealPath(), TRANSFER_MIN_SIZE,
                        true, followSymlink, !disableFileWatchService);
                this.externalOverlays[i] = pr;
            }
        }
    }

    @Override
    public Resource getResource(final String path) throws IOException {
        Resource res = deploymentResourceManager.getResource(path);
        if (res != null) {
            //EE.8.3.1  The content of all jar files in the WEB-INF/lib directory of the containing war
            //file, but not any subdirectories.
            if(res.getPath().startsWith("WEB-INF/lib/") && res.getPath().length() > "WEB-INF/lib/".length() && res.isDirectory()) {
                //if its a directory, treat it as exploded archive, dont list contents
                //NOTE: as is this is being run from caching resource manager, so removal of trailing '/'
                //is not viable
                return null;
            }
            return new ServletResource(this, res);
        }
        String p = path;
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (overlays != null) {
            String canonical = CanonicalPathUtils.canonicalize(p); //we don't need to do this for other resources, as the underlying RM will handle it
            for (VirtualFile overlay : overlays) {
                VirtualFile child = overlay.getChild(canonical);
                if (child.exists()) {
                    try {
                        //we make sure the child is actually a child of the parent
                        //CanonicalPathUtils should make sure this cannot happen
                        //but just to be safe we do it anyway
                        child.getPathNameRelativeTo(overlay);
                        return new ServletResource(this, new VirtualFileResource(overlay.getPhysicalFile(), child, canonical));
                    } catch (IllegalArgumentException ignore) {

                    }
                }
            }
        }
        for (int i = 0; i < externalOverlays.length; ++i) {
            ResourceManager manager = externalOverlays[i];
            res = manager.getResource(path);
            if(res != null) {
                return res;
            }
        }
        return null;
    }

    @Override
    public boolean isResourceChangeListenerSupported() {
        return true;
    }

    @Override
    public void registerResourceChangeListener(ResourceChangeListener listener) {
        if(explodedDeployment && deploymentResourceManager.isResourceChangeListenerSupported()) {
            deploymentResourceManager.registerResourceChangeListener(listener);
        }
        for(ResourceManager external : externalOverlays) {
            if(external.isResourceChangeListenerSupported()) {
                external.registerResourceChangeListener(listener);
            }
        }
    }

    @Override
    public void removeResourceChangeListener(ResourceChangeListener listener) {
        if(deploymentResourceManager.isResourceChangeListenerSupported()) {
            deploymentResourceManager.removeResourceChangeListener(listener);
        }
        for(ResourceManager external : externalOverlays) {
            if(external.isResourceChangeListenerSupported()) {
                external.removeResourceChangeListener(listener);
            }
        }
    }

    @Override
    public void close() throws IOException {
        deploymentResourceManager.close();
    }

    /**
     * Lists all children of a particular path, taking overlays into account
     *
     * @param path The path
     * @return The list of children
     */
    public List<Resource> list(String path) {
        try {
            final List<Resource> ret = new ArrayList<>();

            Resource res = deploymentResourceManager.getResource(path);
            if (res != null) {
                for (Resource child : res.list()) {
                    ret.add(new ServletResource(this, child));
                }
            }
            String p = path;
            if (p.startsWith("/")) {
                p = p.substring(1);
            }
            if (overlays != null) {
                for (VirtualFile overlay : overlays) {
                    VirtualFile child = overlay.getChild(p);
                    if (child.exists()) {
                        VirtualFileResource vfsResource = new VirtualFileResource(overlay.getPhysicalFile(), child, path);
                        for (Resource c : vfsResource.list()) {
                            ret.add(new ServletResource(this, c));
                        }
                    }
                }
            }
            return ret;
        } catch (IOException e) {
            throw new RuntimeException(e); //this method really should have thrown IOException
        }
    }
}
