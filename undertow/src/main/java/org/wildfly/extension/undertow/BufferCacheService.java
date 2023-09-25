/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
