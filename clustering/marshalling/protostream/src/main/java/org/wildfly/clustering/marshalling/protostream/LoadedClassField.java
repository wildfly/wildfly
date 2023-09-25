/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;
import java.io.InvalidClassException;

import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A class field that marshals instances of {@link Class} using a {@link ClassLoaderMarshaller}.
 * @author Paul Ferraro
 */
public class LoadedClassField implements Field<Class<?>>, FieldMarshaller<Class<?>> {

    private final ClassLoaderMarshaller loaderMarshaller;
    private final int index;
    private final int loaderIndex;

    public LoadedClassField(ClassLoaderMarshaller loaderMarshaller, int index) {
        this.loaderMarshaller = loaderMarshaller;
        this.index = index;
        this.loaderIndex = index + 1;
    }

    @Override
    public FieldMarshaller<Class<?>> getMarshaller() {
        return this;
    }

    @Override
    public Class<?> readFrom(ProtoStreamReader reader) throws IOException {
        String className = Scalar.STRING.cast(String.class).readFrom(reader);
        FieldSetReader<ClassLoader> loaderReader = reader.createFieldSetReader(this.loaderMarshaller, this.loaderIndex);
        ClassLoader loader = this.loaderMarshaller.createInitialValue();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            int index = WireType.getTagFieldNumber(tag);
            if (loaderReader.contains(index)) {
                loader = loaderReader.readField(loader);
            } else {
                reader.skipField(tag);
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
        Scalar.STRING.writeTo(writer, targetClass.getName());
        writer.createFieldSetWriter(this.loaderMarshaller, this.loaderIndex).writeFields(WildFlySecurityManager.getClassLoaderPrivileged(targetClass));
    }

    @Override
    public Class<? extends Class<?>> getJavaClass() {
        return ScalarClass.ANY.getJavaClass();
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public WireType getWireType() {
        return WireType.LENGTH_DELIMITED;
    }
}
