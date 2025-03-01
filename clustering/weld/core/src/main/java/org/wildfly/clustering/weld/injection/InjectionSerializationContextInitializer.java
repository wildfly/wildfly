/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.injection;

import org.jboss.weld.injection.EmptyInjectionPoint;
import org.jboss.weld.injection.InjectionPointFactory;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class InjectionSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public InjectionSerializationContextInitializer() {
        super(InjectionPointFactory.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new ConstructorInjectionPointMarshaller<>());
        context.registerMarshaller(ProtoStreamMarshaller.of(EmptyInjectionPoint.INSTANCE));
        context.registerMarshaller(new FieldInjectionPointMarshaller<>());
        context.registerMarshaller(new MethodInjectionPointMarshaller<>());
        context.registerMarshaller(new ParameterInjectionPointMarshaller<>());
    }
}
