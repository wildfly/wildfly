/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import org.jboss.vfs.VirtualFile;

/**
 * Resource manager that deals with overlays
 *
 * @author Stuart Douglas
 */
public class ServletResourceManager implements ResourceManager {

    private final FileResourceManager deploymentResourceManager;
    private final Collection<VirtualFile> overlays;
    private final boolean explodedDeployment;

    public ServletResourceManager(final VirtualFile resourcesRoot, final Collection<VirtualFile> overlays, boolean explodedDeployment, boolean followSymlink) throws IOException {
        this.explodedDeployment = explodedDeployment;
        deploymentResourceManager = new FileResourceManager(resourcesRoot.getPhysicalFile(), 1024 * 1024, followSymlink, "/"); //TODO: enable safe paths support
        this.overlays = overlays;
    }

    @Override
    public Resource getResource(final String path) throws IOException {
        Resource res = deploymentResourceManager.getResource(path);
        if (res != null) {
            return new ServletResource(this, res);
        }
        String p = path;
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if (overlays != null) {
            for (VirtualFile overlay : overlays) {
                VirtualFile child = overlay.getChild(p);
                if (child.exists()) {
                    return new ServletResource(this, new VirtualFileResource(overlay.getPhysicalFile(), child, path));
                }
            }
        }
        return null;
    }

    @Override
    public boolean isResourceChangeListenerSupported() {
        return explodedDeployment;
    }

    @Override
    public void registerResourceChangeListener(ResourceChangeListener listener) {
        deploymentResourceManager.registerResourceChangeListener(listener);
    }

    @Override
    public void removeResourceChangeListener(ResourceChangeListener listener) {
        deploymentResourceManager.removeResourceChangeListener(listener);
    }

    @Override
    public void close() throws IOException {
        deploymentResourceManager.close();
    }

    /**
     * Lists all children of a particular path, taking overlays into account
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
