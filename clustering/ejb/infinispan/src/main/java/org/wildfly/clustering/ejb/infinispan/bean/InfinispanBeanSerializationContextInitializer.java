/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import org.jboss.ejb.client.SessionID;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * A {@link SerializationContextInitializer} that registers marshallers for types in this package.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class InfinispanBeanSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void registerMarshallers(SerializationContext context) {
        ProtoStreamMarshaller<SessionID> sessionIdMarshaller = context.getMarshaller(SessionID.class);
        context.registerMarshaller(sessionIdMarshaller.wrap((Class<InfinispanBeanMetaDataKey<SessionID>>) (Class<?>) InfinispanBeanMetaDataKey.class, InfinispanBeanMetaDataKey::getId, InfinispanBeanMetaDataKey::new));
        context.registerMarshaller(sessionIdMarshaller.wrap((Class<InfinispanBeanGroupKey<SessionID>>) (Class<?>) InfinispanBeanGroupKey.class, InfinispanBeanGroupKey::getId, InfinispanBeanGroupKey::new));
        context.registerMarshaller(Scalar.STRING.cast(String.class).toMarshaller(InfinispanBeanMetaDataFilter.class, Object::toString, InfinispanBeanMetaDataFilter::new));
    }
}
