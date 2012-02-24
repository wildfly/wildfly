/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cmp.jdbc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import org.jboss.as.cmp.CmpMessages;

/**
 * The representation (mapping) in the Java<sup><font size=-2>TM</font></sup>
 * programming language of an SQL <code>BLOB</code> value to an array of bytes.
 * A ByteArrayBlob contains an internal buffer that contains bytes that may be
 * read from the stream. The <code>Blob</code> interface provides methods for
 * getting the length of an SQL <code>BLOB</code> (Binary Large Object) value,
 * for materializing a <code>BLOB</code> value on the client, and for
 * determining the position of a pattern of bytes within a <code>BLOB</code>
 * value. The ByteArrayBlob has static factory methods for constructing an
 * <code>BLOB</code> using either an existing serializable object, or an array
 * of bytes. This is a nice way to store serialized objects in a relational
 * field of type SQL <code>BLOB</code>.
 *
 * @author <a href="mailto:amccullo@sourceforge.new">Andrew McCulloch</a>
 * @author <a href="mailto:abrock@jboss.org">Adrian Brock</a>
 * @version $Revision: 81030 $
 */
public final class ByteArrayBlob implements Blob {
    /**
     * The internal buffer for the bytes of the Blob.
     */
    private final byte[] mBytes;

    public ByteArrayBlob(byte[] bytes) {
        if (bytes == null) {
            bytes = new byte[0];
        }

        mBytes = bytes;
    }

    public InputStream getBinaryStream() throws SQLException {
        return new ByteArrayInputStream(mBytes);
    }

    public byte[] getBytes(long pos, int length) throws SQLException {
        // Defensive code, parameter checks.
        if (length < 0 || length > mBytes.length || pos > mBytes.length) {
            return new byte[0];
        }

        if (pos <= 0) {
            pos = 1; // One since the copy starts at pos.
        }

        byte[] buffer = new byte[length];

        System.arraycopy(mBytes, (int) pos - 1, buffer, 0, length);
        return buffer;
    }

    public long length() throws SQLException {
        return mBytes.length;
    }

    public long position(Blob pattern, long start) throws SQLException {
        return position(pattern.getBytes(0, (int) pattern.length()), start);
    }

    public long position(byte[] pattern, long start) throws SQLException {
        // Small optimization, no need to look beyond this.
        int max = mBytes.length - pattern.length;

        if (start < 0) {
            start = 0; // Cannot start negative, so put it at the beginning.
        } else if (start >= mBytes.length) {
            return -1; // Out of bounds, start was past the end of the buffer.
        }

        if (pattern.length == 0) {
            return -1; // Indicate that the pattern was not found.
        }

        byte first = pattern[0];
        int i = (int) start;

        while (true) {
            // Look for the first character.
            while (i <= max && mBytes[i] != first) {
                i++;
            }

            if (i > max) {
                return -1; // Went to far, reject the pattern.
            }

            // Found the first character, now look for remainder of v2.
            int j = i + 1;
            int end = j + pattern.length - 1;
            int k = 1;
            boolean cont = true;

            // While the bytes remain equal and the end of v1 is not reached
            // continue the either rejecting this match, or accepting it.
            while (cont && j < end) {
                if (mBytes[j++] != pattern[k++]) {
                    i++;
                    cont = false;
                }
            } // If cont == false then the pattern was found.

            if (cont) {
                return i;
            }
        }
    }

    public void free() throws SQLException {
    }

    public InputStream getBinaryStream(long pos, long length) throws SQLException {
        throw CmpMessages.MESSAGES.unimplementedMethod();
    }

    public OutputStream setBinaryStream(long pos)
            throws SQLException {
        throw CmpMessages.MESSAGES.byteArrayBlobIsImmutable();
    }

    public int setBytes(long pos, byte[] bytes)
            throws SQLException {
        throw CmpMessages.MESSAGES.byteArrayBlobIsImmutable();
    }

    public int setBytes(long pos, byte[] bytes, int offset, int length)
            throws SQLException {
        throw CmpMessages.MESSAGES.byteArrayBlobIsImmutable();
    }

    public void truncate(long length)
            throws SQLException {
        throw CmpMessages.MESSAGES.byteArrayBlobIsImmutable();
    }
}

