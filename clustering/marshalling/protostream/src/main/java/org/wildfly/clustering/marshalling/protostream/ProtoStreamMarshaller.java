/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.infinispan.protostream.ProtobufTagMarshaller;

/**
 * A {@link ProtobufTagMarshaller} that include a facility for computing buffer sizes.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller.
 */
public interface ProtoStreamMarshaller<T> extends ProtobufTagMarshaller<T>, Marshallable<T> {

    @Override
    default String getTypeName() {
        Class<?> targetClass = this.getJavaClass();
        Package targetPackage = targetClass.getPackage();
        return (targetPackage != null) ? (targetPackage.getName() + '.' + targetClass.getSimpleName()) : targetClass.getSimpleName();
    }

    @Override
    default T read(ReadContext context) throws IOException {
        return this.readFrom(new DefaultProtoStreamReader(context));
    }

    @Override
    default void write(WriteContext context, T value) throws IOException {
        this.writeTo(new DefaultProtoStreamWriter(context), value);
    }
}
