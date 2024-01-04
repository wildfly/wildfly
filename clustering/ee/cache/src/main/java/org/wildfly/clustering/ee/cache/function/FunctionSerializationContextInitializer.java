/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class FunctionSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new MapComputeFunctionMarshaller());
        context.registerMarshaller(new CollectionFunctionMarshaller<>(SetAddFunction.class, SetAddFunction::new));
        context.registerMarshaller(new CollectionFunctionMarshaller<>(SetRemoveFunction.class, SetRemoveFunction::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(RemappingFunction.class, Scalar.ANY, RemappingFunction::getOperand, RemappingFunction::new));
    }
}
