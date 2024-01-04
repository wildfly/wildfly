/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.jgroups.Address;
import org.wildfly.clustering.marshalling.protostream.FieldSetProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * Provider of marshallers for the org.infinispan.remoting.transport.jgroups package.
 * @author Paul Ferraro
 */
public enum InfinispanJGroupsTransportMarshallerProvider implements ProtoStreamMarshallerProvider {
    JGROUPS_ADDRESS(new FunctionalMarshaller<>(JGroupsAddress.class, new FieldSetProtoStreamMarshaller<>(Address.class, AddressMarshaller.INSTANCE), JGroupsAddress::getJGroupsAddress, JGroupsAddress::new)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    InfinispanJGroupsTransportMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
