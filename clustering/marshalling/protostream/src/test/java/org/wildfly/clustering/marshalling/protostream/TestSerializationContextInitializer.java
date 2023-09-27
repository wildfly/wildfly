/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.Empty;
import org.wildfly.clustering.marshalling.TestComparator;
import org.wildfly.clustering.marshalling.TestInvocationHandler;

/**
 * @author Paul Ferraro
 */
public class TestSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public TestSerializationContextInitializer() {
        super("org.wildfly.clustering.marshalling.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new ValueMarshaller<>(new TestComparator<>()));
        context.registerMarshaller(new EnumMarshaller<>(Empty.class));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(TestInvocationHandler.class, Scalar.ANY, TestInvocationHandler::getValue, TestInvocationHandler::new));
        context.registerMarshaller(new PersonMarshaller());
    }
}
