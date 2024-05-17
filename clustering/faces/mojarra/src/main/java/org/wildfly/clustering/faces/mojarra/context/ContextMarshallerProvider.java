/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.context;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ValueMarshaller;

/**
 * @author Paul Ferraro
 */
public enum ContextMarshallerProvider implements ProtoStreamMarshallerProvider {

    MUTEX(new ValueMarshaller<>(ContextImmutability::createMutex)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    ContextMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
