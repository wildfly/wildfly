/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;

import org.wildfly.clustering.marshalling.protostream.FieldSetProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * Provider of marshallers for the org.jgroups.stack package.
 * @author Paul Ferraro
 */
public enum JGroupsStackMarshallerProvider implements ProtoStreamMarshallerProvider {
    IP_ADDRESS(new FieldSetProtoStreamMarshaller<>(IpAddressMarshaller.INSTANCE)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    JGroupsStackMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
