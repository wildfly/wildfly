/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.ejb;

import org.jboss.as.weld.ejb.SerializedStatefulSessionObject;
import org.jboss.as.weld.ejb.StatefulSessionObjectReferenceImpl;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.ProxyMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class WildFlyWeldEJBSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public WildFlyWeldEJBSerializationContextInitializer() {
        super(SerializedStatefulSessionObject.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new ProxyMarshaller<>(StatefulSessionObjectReferenceImpl.class));
        context.registerMarshaller(new SerializedStatefulSessionObjectMarshaller());
    }
}
