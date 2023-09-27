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
public enum DistributedContextsMarshallerProvider implements ProtoStreamMarshallerProvider {

    PASSIVATION_CAPABLE_SERIALIZABLE_BEAN(new PassivationCapableSerializableBeanMarshaller<>()),
    PASSIVATION_CAPABLE_SERIALIZABLE_CONTEXTUAL(new PassivationCapableSerializableContextualMarshaller<>()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    DistributedContextsMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
