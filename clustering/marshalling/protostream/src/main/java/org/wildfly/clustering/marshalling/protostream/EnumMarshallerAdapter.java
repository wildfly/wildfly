/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

/**
 * Adapts a {@link org.infinispan.protostream.EnumMarshaller} to a {@link ProtoStreamMarshaller}.
 * @author Paul Ferraro
 */
public class EnumMarshallerAdapter<E extends Enum<E>> extends EnumMarshaller<E> {

    private final String typeName;

    @SuppressWarnings("unchecked")
    public EnumMarshallerAdapter(org.infinispan.protostream.EnumMarshaller<E> marshaller) {
        super((Class<E>) marshaller.getJavaClass());
        this.typeName = marshaller.getTypeName();
    }

    @Override
    public String getTypeName() {
        return this.typeName;
    }
}
