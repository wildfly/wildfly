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

package org.jboss.as.server.standalone.management;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.protocol.StreamUtils;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Utility class providing methods for common management tasks.
 *
 * @author John Bailey
 */
public class ManagementUtils {
    private static final MarshallerFactory MARSHALLER_FACTORY;
    private static final MarshallingConfiguration CONFIG;

    static {
        try {
            MARSHALLER_FACTORY = Marshalling.getMarshallerFactory("river", Module.getModuleFromDefaultLoader(ModuleIdentifier.fromString("org.jboss.marshalling.river")).getClassLoader());
            final ClassLoader cl = Module.getModuleFromDefaultLoader(ModuleIdentifier.fromString("org.jboss.as.aggregate")).getClassLoader();
            CONFIG = new MarshallingConfiguration();
            CONFIG.setVersion(2);
            CONFIG.setClassResolver(new SimpleClassResolver(cl));
        } catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
    }

    private ManagementUtils() {
    }

    public static <T> T unmarshal(final ByteInput input, final Class<T> expectedType) throws Exception {
        final Unmarshaller unmarshaller = MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
        unmarshaller.start(input);
        final T result = unmarshaller.readObject(expectedType);
        unmarshaller.finish();
        return result;
    }

    public static void marshal(final ByteOutput output, final Object object) throws Exception {
        final Marshaller marshaller = getMarshaller();
        marshaller.start(output);
        marshaller.writeObject(object);
        marshaller.finish();
    }

    public static Marshaller getMarshaller() throws Exception {
        return MARSHALLER_FACTORY.createMarshaller(CONFIG);
    }

    public static Unmarshaller getUnmarshaller() throws Exception {
        return MARSHALLER_FACTORY.createUnmarshaller(CONFIG);
    }

    public static void expectHeader(final InputStream input, int expected) throws IOException, ManagementException {
        expectHeader(StreamUtils.readByte(input), expected);
    }

    public static void expectHeader(final DataInput input, int expected) throws IOException, ManagementException {
        expectHeader(input.readByte(), expected);
    }

    private static void expectHeader(final byte actual, int expected) throws IOException, ManagementException {
        if (actual != (byte) expected) {
            throw new ManagementException("Invalid byte token.  Expecting '" + expected + "' received '" + actual + "'");
        }
    }

}
