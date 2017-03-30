/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
