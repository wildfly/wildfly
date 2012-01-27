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

package org.jboss.as.protocol.mgmt;

import static org.jboss.as.protocol.ProtocolMessages.MESSAGES;
import org.jboss.as.protocol.StreamUtils;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class providing methods for common management tasks.
 *
 * @author John Bailey
 */
public final class ProtocolUtils {

    public static FlushableDataOutput wrapAsDataOutput(final OutputStream os) {
        return FlushableDataOutputImpl.create(os);
    }

    public static <A> ManagementRequestContext.AsyncTask<A> emptyResponseTask() {
        return new ManagementRequestContext.AsyncTask<A>() {
            @Override
            public void execute(final ManagementRequestContext<A> context) throws Exception {
                final ManagementResponseHeader header = ManagementResponseHeader.create(context.getRequestHeader());
                final FlushableDataOutput output = context.writeMessage(header);
                try {
                    output.writeByte(ManagementProtocol.RESPONSE_END);
                    output.close();
                } finally {
                    StreamUtils.safeClose(output);
                }
            }
        };
    }

    public static <A> void writeResponse(final ResponseWriter writer, final ManagementRequestContext<A> context) throws IOException {
        final ManagementResponseHeader header = ManagementResponseHeader.create(context.getRequestHeader());
        writeResponse(writer, context, header);
    }

    public static <A> void writeResponse(final ResponseWriter writer, final ManagementRequestContext<A> context, final ManagementResponseHeader header) throws IOException {
        final FlushableDataOutput output = context.writeMessage(header);
        try {
            writer.write(output);
            output.writeByte(ManagementProtocol.RESPONSE_END);
            output.close();
        } finally {
            StreamUtils.safeClose(output);
        }

    }

    public static void expectHeader(final InputStream input, int expected) throws IOException {
        expectHeader(readByte(input), expected);
    }

    public static void expectHeader(final DataInput input, int expected) throws IOException {
        expectHeader(input.readByte(), expected);
    }

    public static void expectHeader(final byte actual, int expected) throws IOException {
        if (actual != (byte) expected) {
            throw MESSAGES.invalidByteToken(expected, actual);
        }
    }

    public static int readInt(final InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public static void writeInt(final OutputStream out, final int v) throws IOException {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>>  8) & 0xFF);
        out.write((v >>>  0) & 0xFF);
    }

    public static byte readByte(final InputStream stream) throws IOException {
        int b = stream.read();
        if (b == -1) {
            throw new EOFException();
        }
        return (byte) b;
    }

    public static interface ResponseWriter {

        ResponseWriter EMPTY = new ResponseWriter() {
            @Override
            public void write(FlushableDataOutput output) throws IOException {
                // nothing
            }
        };

        void write(FlushableDataOutput output) throws IOException;

    }

}
