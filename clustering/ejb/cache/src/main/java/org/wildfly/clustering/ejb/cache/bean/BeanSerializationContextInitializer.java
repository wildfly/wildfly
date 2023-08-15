/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.ee.cache.offset.Offset;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;

/**
 * {@link org.infinispan.protostream.SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
public class BeanSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new DefaultBeanMetaDataEntryMarshaller());
        context.registerMarshaller(new FunctionalMarshaller<>(BeanMetaDataEntryFunction.class, Offset.forInstant(Duration.ZERO).getClass().asSubclass(Offset.class), BeanMetaDataEntryFunction::getOffset, BeanMetaDataEntryFunction::new));
    }
}
