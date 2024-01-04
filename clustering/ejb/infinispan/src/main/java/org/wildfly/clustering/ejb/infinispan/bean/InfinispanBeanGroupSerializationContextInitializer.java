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
 * {@link org.infinispan.protostream.SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
public class InfinispanBeanGroupSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalMarshaller<>((Class<InfinispanBeanGroupKey<SessionID>>) (Class<?>) InfinispanBeanGroupKey.class, SessionID.class, InfinispanBeanGroupKey::getId, InfinispanBeanGroupKey::new));
    }
}
