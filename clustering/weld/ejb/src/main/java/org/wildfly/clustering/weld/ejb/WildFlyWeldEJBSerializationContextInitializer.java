/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.ejb;

import org.infinispan.protostream.SerializationContext;
import org.jboss.as.weld.ejb.StatefulSessionObjectReferenceImpl;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.ProxyMarshaller;

/**
 * @author Paul Ferraro
 */
public class WildFlyWeldEJBSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public WildFlyWeldEJBSerializationContextInitializer() {
        super("org.jboss.as.weld.ejb.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new ProxyMarshaller<>(StatefulSessionObjectReferenceImpl.class));
        context.registerMarshaller(new SerializedStatefulSessionObjectMarshaller());
    }
}
