/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import org.jboss.ejb.client.SessionID;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;

/**
 * {@link org.infinispan.protostream.SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
public class InfinispanBeanGroupSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void registerMarshallers(SerializationContext context) {
        ProtoStreamMarshaller<SessionID> sessionIdMarshaller = context.getMarshaller(SessionID.class);
        context.registerMarshaller(sessionIdMarshaller.wrap((Class<InfinispanBeanGroupKey<SessionID>>) (Class<?>) InfinispanBeanGroupKey.class, InfinispanBeanGroupKey::getId, InfinispanBeanGroupKey::new));
    }
}
