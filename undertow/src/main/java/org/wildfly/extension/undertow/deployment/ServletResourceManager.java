package org.wildfly.extension.undertow.deployment;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import org.jboss.vfs.VirtualFile;

/**
 * Resource manager that deals with overlays
 *
 * @author Stuart Douglas
 */
public class ServletResourceManager implements ResourceManager {

    private final VirtualFileResourceManager deploymentResourceManager;
    private final Collection<VirtualFile> overlays;

    public ServletResourceManager(final VirtualFile resourcesRoot, final Collection<VirtualFile> overlays) throws IOException {
        deploymentResourceManager = new VirtualFileResourceManager(resourcesRoot, 1024 * 1024);
        this.overlays = overlays;
    }

    @Override
    public Resource getResource(final String path) throws IOException {
        Resource res = deploymentResourceManager.getResource(path);
        if (res != null) {
            return res;
        }
        String p = path;
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if(overlays != null) {
            for (VirtualFile overlay : overlays) {
                VirtualFile child = overlay.getChild(p);
                if (child.exists()) {
                    URL url = child.toURL();
                    return new URLResource(url, url.openConnection(), path);
                }
            }
        }
        return null;
    }

}
