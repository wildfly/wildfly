package org.wildfly.extension.undertow.deployment;

import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import org.xnio.IoUtils;

import java.io.IOException;
import java.util.List;

/**
 * An Undertow resource manager that attempts to delegate to a list of other resource managers
 * in turn.
 *
 *
 * @author Stuart Douglas
 */
public class DelegatingResourceManager implements ResourceManager {

    private final List<ResourceManager> delegates;
    private final ResourceManager mainDelegate;

    public DelegatingResourceManager(List<ResourceManager> delegates, ResourceManager mainDelegate) {
        this.delegates = delegates;
        this.mainDelegate = mainDelegate;
    }


    @Override
    public Resource getResource(String path) throws IOException {
        for(ResourceManager rm : delegates) {
            Resource res = rm.getResource(path);
            if(res != null) {
                return res;
            }
        }
        return mainDelegate.getResource(path);
    }

    @Override
    public boolean isResourceChangeListenerSupported() {
        return mainDelegate.isResourceChangeListenerSupported();
    }

    @Override
    public void registerResourceChangeListener(ResourceChangeListener listener) {
        mainDelegate.registerResourceChangeListener(listener);
    }

    @Override
    public void removeResourceChangeListener(ResourceChangeListener listener) {
        mainDelegate.registerResourceChangeListener(listener);
    }

    @Override
    public void close() throws IOException {
        for(ResourceManager delegate : delegates) {
            IoUtils.safeClose(delegate);
        }
    }
}
