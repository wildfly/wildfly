/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.io.InvalidClassException;

import org.infinispan.protostream.impl.WireFormat;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A class field that marshals instances of {@link Class} using a {@link ClassResolver}.
 * @author Paul Ferraro
 */
public class LoadedClassField implements Field<Class<?>> {

    private final ClassLoaderMarshaller loaderMarshaller;
    private final int index;
    private final int loaderIndex;

    public LoadedClassField(ClassLoaderMarshaller loaderMarshaller, int index) {
        this.loaderMarshaller = loaderMarshaller;
        this.index = index;
        this.loaderIndex = index + 1;
    }

    @Override
    public Class<?> readFrom(ProtoStreamReader reader) throws IOException {
        String className = reader.readString();
        ClassLoader loader = this.loaderMarshaller.getBuilder();
        boolean reading = true;
        while (reading) {
            int tag = reader.readTag();
            int index = WireFormat.getTagFieldNumber(tag);
            if ((index >= this.loaderIndex) && (index < this.loaderIndex + this.loaderMarshaller.getFields())) {
                loader = this.loaderMarshaller.readField(reader, index, loader);
            } else {
                reading = (tag != 0) && reader.skipField(tag);
            }
        }
        try {
            return loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            InvalidClassException exception = new InvalidClassException(e.getLocalizedMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Class<?> targetClass) throws IOException {
        writer.writeStringNoTag(targetClass.getName());
        this.loaderMarshaller.writeFields(writer, this.loaderIndex, WildFlySecurityManager.getClassLoaderPrivileged(targetClass));
    }

    @Override
    public Class<? extends Class<?>> getJavaClass() {
        return ClassField.ANY.getJavaClass();
    }

    @Override
    public int getIndex() {
        return this.index;
    }
}
