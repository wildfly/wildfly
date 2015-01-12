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
package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;

/**
 * Simple {@link ClassTable} implementation based on an array of recognized classes.
 * @author Paul Ferraro
 */
public class SimpleClassTable implements ClassTable {

    private final Class<?>[] classes;
    private final Map<Class<?>, Writer> writers = new IdentityHashMap<>();

    public SimpleClassTable(Class<?>... classes) {
        this.classes = classes;
        for (int i = 0; i < classes.length; i++) {
            this.writers.put(classes[i], new ByteWriter((byte) i));
        }
    }

    @Override
    public Writer getClassWriter(Class<?> clazz) {
        return this.writers.get(clazz);
    }

    @Override
    public Class<?> readClass(Unmarshaller unmarshaller) throws IOException {
        return this.classes[unmarshaller.readUnsignedByte()];
    }

    private static final class ByteWriter implements ClassTable.Writer {
        final byte[] bytes;

        ByteWriter(byte... bytes) {
            this.bytes = bytes;
        }

        @Override
        public void writeClass(Marshaller marshaller, Class<?> clazz) throws IOException {
            marshaller.write(this.bytes);
        }
    }
}
