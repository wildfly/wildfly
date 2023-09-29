/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.function;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class FunctionSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new MapComputeFunctionMarshaller());
        context.registerMarshaller(new CollectionFunctionMarshaller<>(SetAddFunction.class, SetAddFunction::new));
        context.registerMarshaller(new CollectionFunctionMarshaller<>(SetRemoveFunction.class, SetRemoveFunction::new));
        // Deprecated functions, to be removed
        context.registerMarshaller(new FunctionalMarshaller<>(ConcurrentMapPutFunction.class, (Class<Map.Entry<Object, Object>>) (Class<?>) SimpleImmutableEntry.class, ConcurrentMapPutFunction<Object, Object>::getEntry, ConcurrentMapPutFunction<Object, Object>::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(ConcurrentMapRemoveFunction.class, Scalar.ANY, ConcurrentMapRemoveFunction::getKey, ConcurrentMapRemoveFunction::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(ConcurrentSetAddFunction.class, Scalar.ANY, ConcurrentSetAddFunction::getValue, ConcurrentSetAddFunction::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(ConcurrentSetRemoveFunction.class, Scalar.ANY, ConcurrentSetRemoveFunction::getValue, ConcurrentSetRemoveFunction::new));
        context.registerMarshaller(new FunctionalMarshaller<>(CopyOnWriteMapPutFunction.class, (Class<Map.Entry<Object, Object>>) (Class<?>) SimpleImmutableEntry.class, CopyOnWriteMapPutFunction<Object, Object>::getEntry, CopyOnWriteMapPutFunction<Object, Object>::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(CopyOnWriteMapRemoveFunction.class, Scalar.ANY, CopyOnWriteMapRemoveFunction::getKey, CopyOnWriteMapRemoveFunction::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(CopyOnWriteSetAddFunction.class, Scalar.ANY, CopyOnWriteSetAddFunction::getValue, CopyOnWriteSetAddFunction::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(CopyOnWriteSetRemoveFunction.class, Scalar.ANY, CopyOnWriteSetRemoveFunction::getValue, CopyOnWriteSetRemoveFunction::new));
    }
}
