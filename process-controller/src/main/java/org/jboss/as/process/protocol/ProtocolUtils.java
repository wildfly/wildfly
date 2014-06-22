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

import org.jboss.as.process.logging.ProcessLogger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class providing methods for common management tasks.
 *
 * @author John Bailey
 */
public class    ProtocolUtils {
    private static final MarshallerFactory MARSHALLER_FACTORY;

    static {
        MARSHALLER_FACTORY = Marshalling.getMarshallerFactory("river", ProtocolUtils.class.getClassLoader());
    }

    private ProtocolUtils() {
    }

    public static <T> T unmarshal(final Unmarshaller unmarshaller, final Class<T> expectedType) throws IOException {
        try {
            return unmarshaller.readObject(expectedType);
        } catch (ClassNotFoundException e) {
            throw ProcessLogger.ROOT_LOGGER.failedToReadObject(e);
        }
    }

    public static Marshaller getMarshaller(final MarshallingConfiguration config) throws IOException {
        return MARSHALLER_FACTORY.createMarshaller(config);
    }

    public static Unmarshaller getUnmarshaller(final MarshallingConfiguration config) throws IOException {
        return MARSHALLER_FACTORY.createUnmarshaller(config);
    }

    public static void expectHeader(final InputStream input, int expected) throws IOException {
        expectHeader(StreamUtils.readByte(input), expected);
    }

    public static void expectHeader(final DataInput input, int expected) throws IOException {
        expectHeader(input.readByte(), expected);
    }

    public static void expectHeader(final byte actual, int expected) throws IOException {
        if (actual != (byte) expected) {
            throw ProcessLogger.ROOT_LOGGER.invalidByteToken(expected, actual);
        }
    }
}
