/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.client.impl;

import org.jboss.as.protocol.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Emanuel Muckenhuber
 */
public interface InputStreamEntry extends Closeable {

    /**
     * Initialize the input stream entry.
     *
     * @return the size of the underlying stream
     */
    int initialize() throws IOException;

    /**
     * Copy the stream.
     *
     * @param output the data output
     * @throws IOException for any error
     */
    void copyStream(DataOutput output) throws IOException;

    /**
     * Copy the data in-memory.
     */
    class InMemoryEntry implements InputStreamEntry {

        private final boolean autoClose;
        private final InputStream original;

        private byte[] data;

        public InMemoryEntry(InputStream original, boolean autoClose) {
            this.original = original;
            this.autoClose = autoClose;
        }

        @Override
        public synchronized int initialize() throws IOException {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                StreamUtils.copyStream(original, os);
            } finally {
                if(autoClose) {
                    StreamUtils.safeClose(original);
                }
            }
            data = os.toByteArray();
            return data.length;
        }

        @Override
        public synchronized void copyStream(final DataOutput output) throws IOException {
            try {
                output.write(data);
            } finally {
                data = null;
            }
        }

        @Override
        public void close() throws IOException {
            //
        }
    }

    /**
     * Cache the data on disk.
     */
    class CachingStreamEntry implements InputStreamEntry {

        private final boolean autoClose;
        private final InputStream original;

        private File temp;

        public CachingStreamEntry(final InputStream original, final boolean autoClose) {
            this.original = original;
            this.autoClose = autoClose;
        }

        public synchronized int initialize() throws IOException {
            if(temp == null) {
                temp = File.createTempFile("client", "stream");
                temp.deleteOnExit();
                final FileOutputStream os = new FileOutputStream(temp);
                try {
                    StreamUtils.copyStream(original, os);
                    os.flush();
                    os.close();
                } finally {
                    StreamUtils.safeClose(os);
                    if(autoClose) {
                        StreamUtils.safeClose(original);
                    }
                }
            }
            return (int) temp.length();
        }

        @Override
        public synchronized void copyStream(final DataOutput output) throws IOException {
            final FileInputStream is = new FileInputStream(temp);
            try {
                StreamUtils.copyStream(is, output);
            } finally {
                StreamUtils.safeClose(is);
            }
        }

        @Override
        public synchronized void close() throws IOException {
            temp.delete();
            temp = null;
        }
    }

    InputStreamEntry EMPTY = new InputStreamEntry() {
        @Override
        public int initialize() throws IOException {
            return 0;
        }

        @Override
        public void copyStream(final DataOutput output) throws IOException {
            output.write(new byte[0]);
        }

        @Override
        public void close() throws IOException {
            //
        }
    };

}
