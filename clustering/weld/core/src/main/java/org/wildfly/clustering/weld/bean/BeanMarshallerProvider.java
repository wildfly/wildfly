/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.bean;

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.bean.ManagedBeanIdentifier;
import org.jboss.weld.bean.StringBeanIdentifier;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.reflect.UnaryFieldMarshaller;

/**
 * @author Paul Ferraro
 */
public enum BeanMarshallerProvider implements ProtoStreamMarshallerProvider {

    MANAGED_BEAN_IDENTIFIER(new UnaryFieldMarshaller<>(ManagedBeanIdentifier.class, AnnotatedTypeIdentifier.class, ManagedBeanIdentifier::new)),
    STRING_BEAN_IDENTIFIER(new FunctionalScalarMarshaller<>(StringBeanIdentifier.class, Scalar.STRING.cast(String.class), StringBeanIdentifier::asString, StringBeanIdentifier::new)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    BeanMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
