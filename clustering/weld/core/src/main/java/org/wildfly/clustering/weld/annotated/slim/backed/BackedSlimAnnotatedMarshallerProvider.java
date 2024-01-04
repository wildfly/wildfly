/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.annotated.slim.backed;

import org.jboss.weld.annotated.slim.backed.BackedAnnotatedConstructor;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedField;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedMethod;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedParameter;
import org.jboss.weld.annotated.slim.backed.BackedAnnotatedType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedConstructorMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedFieldMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedMethodMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedParameterMarshaller;

/**
 * @author Paul Ferraro
 */
public enum BackedSlimAnnotatedMarshallerProvider implements ProtoStreamMarshallerProvider {

    CONSTRUCTOR(new AnnotatedConstructorMarshaller<>(BackedAnnotatedConstructor.class, BackedAnnotatedType.class)),
    FIELD(new AnnotatedFieldMarshaller<>(BackedAnnotatedField.class, BackedAnnotatedType.class)),
    METHOD(new AnnotatedMethodMarshaller<>(BackedAnnotatedMethod.class, BackedAnnotatedType.class)),
    PARAMETER(new AnnotatedParameterMarshaller<>(BackedAnnotatedParameter.class, BackedAnnotatedConstructor.class, BackedAnnotatedMethod.class)),
    TYPE(new BackedAnnotatedTypeMarshaller<>()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    BackedSlimAnnotatedMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
