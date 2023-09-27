/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import org.xnio.IoUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class DelegatingResourceManager implements ResourceManager {

    private final List<ResourceManager> delegates;

    public DelegatingResourceManager(List<ResourceManager> delegates) {
        this.delegates = new ArrayList<>(delegates);
    }

    @Override
    public Resource getResource(String path) throws IOException {
        for(ResourceManager d : delegates) {
            Resource res = d.getResource(path);
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
        for(ResourceManager del : delegates) {
            if(del.isResourceChangeListenerSupported()) {
                del.registerResourceChangeListener(listener);
            }
        }
    }

    @Override
    public void removeResourceChangeListener(ResourceChangeListener listener) {
        for(ResourceManager del : delegates) {
            if(del.isResourceChangeListenerSupported()) {
                del.removeResourceChangeListener(listener);
            }
        }
    }

    @Override
    public void close() throws IOException {
        for(ResourceManager del : delegates) {
            IoUtils.safeClose(del);
        }
    }
}
