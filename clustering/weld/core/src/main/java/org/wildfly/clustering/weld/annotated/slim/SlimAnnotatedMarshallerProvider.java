/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.annotated.slim;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum SlimAnnotatedMarshallerProvider implements ProtoStreamMarshallerProvider {

    IDENTIFIER(new AnnotatedTypeIdentifierMarshaller()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    SlimAnnotatedMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
