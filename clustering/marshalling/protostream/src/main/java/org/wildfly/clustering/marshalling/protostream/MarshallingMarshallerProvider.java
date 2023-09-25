/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

/**
 * @author Paul Ferraro
 */
public enum MarshallingMarshallerProvider implements ProtoStreamMarshallerProvider {
    BYTE_BUFFER_MARSHALLED_KEY(new ByteBufferMarshalledKeyMarshaller()),
    BYTE_BUFFER_MARSHALLED_VALUE(new ByteBufferMarshalledValueMarshaller()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    MarshallingMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
