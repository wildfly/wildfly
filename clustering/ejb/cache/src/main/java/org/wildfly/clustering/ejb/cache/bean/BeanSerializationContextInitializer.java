/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;
import java.time.Instant;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.offset.Offset;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * {@link org.infinispan.protostream.SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class BeanSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new DefaultBeanMetaDataEntryMarshaller());
        @SuppressWarnings("unchecked")
        ProtoStreamMarshaller<Offset<Instant>> offsetMarshaller = context.getMarshaller((Class<Offset<Instant>>) Offset.forInstant(Duration.ZERO).getClass());
        context.registerMarshaller(offsetMarshaller.wrap(BeanMetaDataEntryFunction.class, BeanMetaDataEntryFunction::getOffset, BeanMetaDataEntryFunction::new));
    }
}
