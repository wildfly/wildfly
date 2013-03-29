package org.jboss.as.undertow;

import java.nio.ByteBuffer;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
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

    public BufferPoolService(int bufferSize, int buffersPerSlice) {
        this.bufferSize = bufferSize;
        this.buffersPerSlice = buffersPerSlice;
    }

    @Override
    public void start(StartContext context) throws StartException {
        bufferPool = new ByteBufferSlicePool(bufferSize, buffersPerSlice * bufferSize);
    }

    @Override
    public void stop(StopContext context) {

    }

    @Override
    public Pool<ByteBuffer> getValue() throws IllegalStateException, IllegalArgumentException {
        return bufferPool;
    }
}
