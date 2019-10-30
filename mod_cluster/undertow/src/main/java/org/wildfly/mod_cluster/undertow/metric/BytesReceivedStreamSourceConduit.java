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

import org.xnio.channels.StreamSinkChannel;
import org.xnio.conduits.AbstractSourceConduit;
import org.xnio.conduits.StreamSourceConduit;

/**
 * Implementation of {@link StreamSourceConduit} wrapping that wraps around byte-transferring methods to calculate total
 * number of bytes transferred leveraging {@link LongAdder}.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public class BytesReceivedStreamSourceConduit extends AbstractSourceConduit implements StreamSourceConduit {

    private final StreamSourceConduit next;
    private static final LongAdder bytesReceived = new LongAdder();

    public BytesReceivedStreamSourceConduit(StreamSourceConduit next) {
        super(next);
        this.next = next;
    }

    @Override
    public long transferTo(long position, long count, FileChannel target) throws IOException {
        long bytes = next.transferTo(position, count, target);
        bytesReceived.add(bytes);
        return bytes;
    }

    @Override
    public long transferTo(long count, ByteBuffer throughBuffer, StreamSinkChannel target) throws IOException {
        long bytes = next.transferTo(count, throughBuffer, target);
        bytesReceived.add(bytes);
        return bytes;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int bytes = next.read(dst);
        bytesReceived.add(bytes);
        return bytes;
    }

    @Override
    public long read(ByteBuffer[] dsts, int offs, int len) throws IOException {
        long bytes = next.read(dsts, offs, len);
        bytesReceived.add(bytes);
        return bytes;
    }

    public static long getBytesReceived() {
        return bytesReceived.longValue();
    }
}


