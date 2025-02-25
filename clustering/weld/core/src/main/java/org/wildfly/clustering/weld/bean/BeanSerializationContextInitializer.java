/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.bean;

import org.jboss.weld.annotated.slim.AnnotatedTypeIdentifier;
import org.jboss.weld.bean.ManagedBeanIdentifier;
import org.jboss.weld.bean.StringBeanIdentifier;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.UnaryFieldMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class BeanSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public BeanSerializationContextInitializer() {
        super(ManagedBeanIdentifier.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new UnaryFieldMarshaller<>(ManagedBeanIdentifier.class, AnnotatedTypeIdentifier.class, ManagedBeanIdentifier::new));
        context.registerMarshaller(Scalar.STRING.cast(String.class).toMarshaller(StringBeanIdentifier.class, StringBeanIdentifier::asString, StringBeanIdentifier::new));
    }
}
