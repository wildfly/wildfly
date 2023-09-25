/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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


