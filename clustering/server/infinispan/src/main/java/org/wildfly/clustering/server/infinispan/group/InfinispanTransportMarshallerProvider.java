/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import org.infinispan.remoting.transport.LocalModeAddress;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.ValueMarshaller;

/**
 * Provider of marshallers for the org.infinispan.remoting.transport package.
 * @author Paul Ferraro
 */
public enum InfinispanTransportMarshallerProvider implements ProtoStreamMarshallerProvider {
    LOCAL_ADDRESS(new ValueMarshaller<>(LocalModeAddress.INSTANCE)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    InfinispanTransportMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
