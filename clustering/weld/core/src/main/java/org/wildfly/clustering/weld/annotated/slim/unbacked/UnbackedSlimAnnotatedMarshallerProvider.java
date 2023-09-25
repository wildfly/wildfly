/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.annotated.slim.unbacked;

import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedConstructor;
import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedField;
import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedMethod;
import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedParameter;
import org.jboss.weld.annotated.slim.unbacked.UnbackedAnnotatedType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedConstructorMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedFieldMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedMethodMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedParameterMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedTypeMarshaller;

/**
 * @author Paul Ferraro
 */
public enum UnbackedSlimAnnotatedMarshallerProvider implements ProtoStreamMarshallerProvider {

    CONSTRUCTOR(new AnnotatedConstructorMarshaller<>(UnbackedAnnotatedConstructor.class, UnbackedAnnotatedType.class)),
    FIELD(new AnnotatedFieldMarshaller<>(UnbackedAnnotatedField.class, UnbackedAnnotatedType.class)),
    METHOD(new AnnotatedMethodMarshaller<>(UnbackedAnnotatedMethod.class, UnbackedAnnotatedType.class)),
    PARAMETER(new AnnotatedParameterMarshaller<>(UnbackedAnnotatedParameter.class, UnbackedAnnotatedConstructor.class, UnbackedAnnotatedMethod.class)),
    TYPE(new AnnotatedTypeMarshaller<>(UnbackedAnnotatedType.class)),
    IDENTIFIER(new UnbackedMemberIdentifierMarshaller<>()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    UnbackedSlimAnnotatedMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
