/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;

/**
 * {@link org.infinispan.protostream.SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
public class BeanSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new SimpleBeanCreationMetaDataMarshaller());
        context.registerMarshaller(new FunctionalMarshaller<>(SimpleBeanAccessMetaData.class, Duration.class, SimpleBeanAccessMetaData::getLastAccessDuration, SimpleBeanAccessMetaData::valueOf));
    }
}
