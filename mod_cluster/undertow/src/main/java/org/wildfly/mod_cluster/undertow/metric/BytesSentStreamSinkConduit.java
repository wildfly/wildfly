/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.mod_cluster.undertow.metric;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.LongAdder;

import org.xnio.channels.StreamSourceChannel;
import org.xnio.conduits.AbstractSinkConduit;
import org.xnio.conduits.StreamSinkConduit;

/**
 * Implementation of {@link StreamSinkConduit} wrapping that wraps around byte-transferring methods to calculate total
 * number of bytes transferred leveraging {@link LongAdder}.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public class BytesSentStreamSinkConduit extends AbstractSinkConduit implements StreamSinkConduit {

    private final StreamSinkConduit next;
    private static final LongAdder bytesSent = new LongAdder();

    public BytesSentStreamSinkConduit(StreamSinkConduit next) {
        super(next);
        this.next = next;
    }

    @Override
    public long transferFrom(FileChannel src, long position, long count) throws IOException {
        long bytes = next.transferFrom(src, position, count);
        bytesSent.add(bytes);
        return bytes;
    }


    @Override
    public long transferFrom(StreamSourceChannel source, long count, ByteBuffer throughBuffer) throws IOException {
        long bytes = next.transferFrom(source, count, throughBuffer);
        bytesSent.add(bytes);
        return bytes;
    }


    @Override
    public int write(ByteBuffer src) throws IOException {
        int bytes = next.write(src);
        bytesSent.add(bytes);
        return bytes;
    }


    @Override
    public long write(ByteBuffer[] srcs, int offs, int len) throws IOException {
        long bytes = next.write(srcs, offs, len);
        bytesSent.add(bytes);
        return bytes;
    }

    @Override
    public int writeFinal(ByteBuffer src) throws IOException {
        int bytes = next.writeFinal(src);
        bytesSent.add(bytes);
        return bytes;
    }

    @Override
    public long writeFinal(ByteBuffer[] srcs, int offset, int length) throws IOException {
        long bytes = next.writeFinal(srcs, offset, length);
        bytesSent.add(bytes);
        return bytes;
    }

    public static long getBytesSent() {
        return bytesSent.longValue();
    }
}
