/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.io;

import java.nio.ByteBuffer;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.xnio.BufferAllocator;
import org.xnio.ByteBufferSlicePool;
import org.xnio.Pool;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class BufferPoolService implements Service<Pool<ByteBuffer>> {
    private volatile Pool<ByteBuffer> bufferPool;
    /*<buffer-pool name="default" buffer-size="2048" buffers-per-slice="512"/>*/
    private final int bufferSize;
    private final int buffersPerSlice;
    private final boolean directBuffers;

    public BufferPoolService(int bufferSize, int buffersPerSlice, final boolean directBuffers) {
        this.bufferSize = bufferSize;
        this.buffersPerSlice = buffersPerSlice;
        this.directBuffers = directBuffers;
    }

    @Override
    public void start(StartContext context) throws StartException {
        bufferPool = new ByteBufferSlicePool(directBuffers ? BufferAllocator.DIRECT_BYTE_BUFFER_ALLOCATOR : BufferAllocator.BYTE_BUFFER_ALLOCATOR, bufferSize, buffersPerSlice * bufferSize);
    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public Pool<ByteBuffer> getValue() throws IllegalStateException, IllegalArgumentException {
        return bufferPool;
    }
}
