/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.injection;

import org.jboss.weld.injection.EmptyInjectionPoint;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ValueMarshaller;

/**
 * @author Paul Ferraro
 */
public enum InjectionMarshallerProvider implements ProtoStreamMarshallerProvider {

    CONSTRUCTOR(new ConstructorInjectionPointMarshaller<>()),
    EMPTY(new ValueMarshaller<>(EmptyInjectionPoint.INSTANCE)),
    FIELD(new FieldInjectionPointMarshaller<>()),
    METHOD(new MethodInjectionPointMarshaller<>()),
    PARAMETER(new ParameterInjectionPointMarshaller<>()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    InjectionMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
