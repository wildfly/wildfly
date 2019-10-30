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

package org.wildfly.extension.undertow;

import io.undertow.server.handlers.cache.DirectBufferCache;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Stuart Douglas
 */
public class BufferCacheService implements Service<DirectBufferCache> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("undertow", "bufferCache");

    private final int bufferSize;
    private final int buffersPerRegion;
    private final int maxRegions;

    private volatile DirectBufferCache value;

    public BufferCacheService(final int bufferSize, final int buffersPerRegion, final int maxRegions) {
        this.bufferSize = bufferSize;
        this.buffersPerRegion = buffersPerRegion;
        this.maxRegions = maxRegions;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        value = new DirectBufferCache(bufferSize, buffersPerRegion, maxRegions * buffersPerRegion * bufferSize);
    }

    @Override
    public void stop(final StopContext stopContext) {
        value = null;
    }

    @Override
    public DirectBufferCache getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }
}
