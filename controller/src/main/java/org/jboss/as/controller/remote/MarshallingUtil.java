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

package org.jboss.as.controller.remote;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

/**
 * A utility for creating the {@link Marshaller} and {@link Unmarshaller}.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class MarshallingUtil {

    private static final String FACTORY_NAME = "river";
    private static final MarshallingConfiguration configuration = new MarshallingConfiguration();

    static Marshaller getMarshaller() throws IOException {
        return getMarshallerFactory().createMarshaller(configuration);
    }

    static ByteOutput createByteOutput(final DataOutput dataOutput) {
        final OutputStream outputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                final int byteToWrite = b & 0xff;
                dataOutput.write(byteToWrite);
            }
        };

        return Marshalling.createByteOutput(outputStream);
    }

    static Unmarshaller getUnmarshaller() throws IOException {
        return getMarshallerFactory().createUnmarshaller(configuration);
    }

    static ByteInput createByteInput(final DataInput dataInput) {
        final InputStream is = new InputStream() {
            @Override
            public int read() throws IOException {
                try {

                    final int b = dataInput.readByte();
                    return b & 0xff;
                } catch (EOFException eof) {
                    return -1;
                }
            }
        };

        return Marshalling.createByteInput(is);
    }

    private static MarshallerFactory getMarshallerFactory() throws IOException {
        MarshallerFactory factory = Marshalling.getMarshallerFactory(FACTORY_NAME, MarshallingUtil.class.getClassLoader());

        return factory;
    }

}
