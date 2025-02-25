/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.bean.proxy.util;

import org.jboss.weld.bean.proxy.util.SerializableClientProxy;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.BinaryFieldMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class UtilProxyBeanSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public UtilProxyBeanSerializationContextInitializer() {
        super(SerializableClientProxy.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new BinaryFieldMarshaller<>(SerializableClientProxy.class, BeanIdentifier.class, String.class, SerializableClientProxy::new));
    }
}
