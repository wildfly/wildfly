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
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedConstructorMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedFieldMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedMethodMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedParameterMarshaller;
import org.wildfly.clustering.weld.annotated.slim.AnnotatedTypeMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class UnbackedSlimAnnotatedMarshallerProvider extends AbstractSerializationContextInitializer {

    public UnbackedSlimAnnotatedMarshallerProvider() {
        super(UnbackedAnnotatedType.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new AnnotatedConstructorMarshaller<>(UnbackedAnnotatedConstructor.class, UnbackedAnnotatedType.class));
        context.registerMarshaller(new AnnotatedFieldMarshaller<>(UnbackedAnnotatedField.class, UnbackedAnnotatedType.class));
        context.registerMarshaller(new AnnotatedMethodMarshaller<>(UnbackedAnnotatedMethod.class, UnbackedAnnotatedType.class));
        context.registerMarshaller(new AnnotatedParameterMarshaller<>(UnbackedAnnotatedParameter.class, UnbackedAnnotatedConstructor.class, UnbackedAnnotatedMethod.class));
        context.registerMarshaller(new AnnotatedTypeMarshaller<>(UnbackedAnnotatedType.class));
        context.registerMarshaller(new UnbackedMemberIdentifierMarshaller<>());
    }
}
