/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum ContextsMarshallerProvider implements ProtoStreamMarshallerProvider {

    CREATIONAL_CONTEXT_IMPL(new CreationalContextImplMarshaller<>()),
    SERIALIZABLE_CONTEXTUAL_INSTANCE(new SerializableContextualInstanceImplMarshaller<>()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    ContextsMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
