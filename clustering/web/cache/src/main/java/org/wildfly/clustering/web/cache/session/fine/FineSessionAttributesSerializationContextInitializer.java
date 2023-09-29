/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.fine;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;

/**
 * {@link SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class FineSessionAttributesSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalMarshaller<>(ConcurrentSessionAttributeMapPutFunction.class, SessionAttributeMapEntryMarshaller.INSTANCE, ConcurrentSessionAttributeMapPutFunction::getEntry, ConcurrentSessionAttributeMapPutFunction::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(ConcurrentSessionAttributeMapRemoveFunction.class, Scalar.STRING.cast(String.class), ConcurrentSessionAttributeMapRemoveFunction::getKey, ConcurrentSessionAttributeMapRemoveFunction::new));
        context.registerMarshaller(new FunctionalMarshaller<>(CopyOnWriteSessionAttributeMapPutFunction.class, SessionAttributeMapEntryMarshaller.INSTANCE, CopyOnWriteSessionAttributeMapPutFunction::getEntry, CopyOnWriteSessionAttributeMapPutFunction::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(CopyOnWriteSessionAttributeMapRemoveFunction.class, Scalar.STRING.cast(String.class), CopyOnWriteSessionAttributeMapRemoveFunction::getKey, CopyOnWriteSessionAttributeMapRemoveFunction::new));
    }
}
