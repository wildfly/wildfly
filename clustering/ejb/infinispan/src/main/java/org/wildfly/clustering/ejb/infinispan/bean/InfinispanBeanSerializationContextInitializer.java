/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import org.infinispan.protostream.SerializationContext;
import org.jboss.ejb.client.SessionID;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;

/**
 * A {@link SerializationContextInitializer} that registers marshallers for types in this package.
 * @author Paul Ferraro
 */
public class InfinispanBeanSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalMarshaller<>((Class<InfinispanBeanCreationMetaDataKey<SessionID>>) (Class<?>) InfinispanBeanCreationMetaDataKey.class, SessionID.class, InfinispanBeanCreationMetaDataKey::getId, InfinispanBeanCreationMetaDataKey::new));
        context.registerMarshaller(new FunctionalMarshaller<>((Class<InfinispanBeanAccessMetaDataKey<SessionID>>) (Class<?>) InfinispanBeanAccessMetaDataKey.class, SessionID.class, InfinispanBeanAccessMetaDataKey::getId, InfinispanBeanAccessMetaDataKey::new));
        context.registerMarshaller(new FunctionalMarshaller<>((Class<InfinispanBeanGroupKey<SessionID>>) (Class<?>) InfinispanBeanGroupKey.class, SessionID.class, InfinispanBeanGroupKey::getId, InfinispanBeanGroupKey::new));
    }
}
