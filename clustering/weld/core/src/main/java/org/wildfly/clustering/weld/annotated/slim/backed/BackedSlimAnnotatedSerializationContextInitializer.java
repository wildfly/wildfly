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
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedConstructorMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedFieldMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedMethodMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedParameterMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class BackedSlimAnnotatedSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public BackedSlimAnnotatedSerializationContextInitializer() {
        super(BackedAnnotatedType.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new AnnotatedConstructorMarshaller<>(BackedAnnotatedConstructor.class, BackedAnnotatedType.class));
        context.registerMarshaller(new AnnotatedFieldMarshaller<>(BackedAnnotatedField.class, BackedAnnotatedType.class));
        context.registerMarshaller(new AnnotatedMethodMarshaller<>(BackedAnnotatedMethod.class, BackedAnnotatedType.class));
        context.registerMarshaller(new AnnotatedParameterMarshaller<>(BackedAnnotatedParameter.class, BackedAnnotatedConstructor.class, BackedAnnotatedMethod.class));
        context.registerMarshaller(new BackedAnnotatedTypeMarshaller<>());
    }
}
