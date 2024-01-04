/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.bean.proxy.util;

import org.jboss.weld.bean.proxy.util.SerializableClientProxy;
import org.jboss.weld.serialization.spi.BeanIdentifier;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.reflect.BinaryFieldMarshaller;

/**
 * @author Paul Ferraro
 */
public enum UtilProxyBeanMarshallerProvider implements ProtoStreamMarshallerProvider {

    SERIALIZABLE_CLIENT_PROXY(new BinaryFieldMarshaller<>(SerializableClientProxy.class, BeanIdentifier.class, String.class, SerializableClientProxy::new)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    UtilProxyBeanMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
