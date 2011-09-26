/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.process.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PipeTest {

    private static final long SEED = 123L;

    @Test
    public void testClose() throws Exception {
        Pipe pipe = new Pipe(8192);
        InputStream in = pipe.getIn();
        OutputStream out = pipe.getOut();
        out.close();
        try {
            out.write(0);
            fail("Expected exception");
        } catch (IOException expected) {
        }
        assertEquals(-1, in.read());
        // should be idempotent
        out.close();
        // should not throw exception
        in.close();
        // should return immediately
        pipe.await();
    }

    static int[] ints(int... v) {
        return v;
    }

    @Test
    public void testIntegrity() throws Exception {
        Thread thread = null;
        try {
            for (final int bufSize : ints(31, 256, 8192)) {
                for (final int pieceSize : ints(31, 64, 127)) {
                    final int pieceCnt = (1 << 17) / pieceSize;
                    System.out.printf("Buffer size: %d; piece size: %d; piece count %d\n", Integer.valueOf(bufSize), Integer.valueOf(pieceSize), Integer.valueOf(pieceCnt));
                    final int finalSize = pieceCnt * pieceSize;

                    final Pipe pipe = new Pipe(bufSize);
                    InputStream in = pipe.getIn();

                    // fire up the write thread
                    thread = new Thread(new Runnable() {
                        public void run() {
                            final Random rng = new Random(SEED);
                            final byte[] piece = new byte[pieceSize];
                            try {
                                OutputStream out = pipe.getOut();
                                for (int i = 0; i < pieceCnt; i++) {
                                    rng.nextBytes(piece);
                                    out.write(piece);
                                }
                                out.close();
                                pipe.await();
                            } catch (IOException e) {
                                e.printStackTrace(System.err);
                                System.err.flush();
                            }
                        }
                    }, "Test write thread");
                    thread.setDaemon(true);
                    thread.start();
                    final Random rng1 = new Random(SEED);
                    int remaining = finalSize;
                    while (remaining > 0) {
                        byte[] buf1 = new byte[Math.min(pieceSize, remaining)];
                        byte[] buf2 = new byte[buf1.length];
                        try {
                            StreamUtils.readFully(in, buf1);
                        } catch (IOException e) {
                            thread.isDaemon();
                        }
                        rng1.nextBytes(buf2);
                        assertArrayEquals("Failure at remaining = " + remaining, buf1, buf2);
                        remaining -= buf1.length;
                    }
                    assertEquals(-1, in.read());
                    in.close();
                }
            }
        } finally {
            if (thread != null) {
                thread.join(1000L);
            }
        }
    }
}
