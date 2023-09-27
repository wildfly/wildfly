/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.group;


import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * Provider of marshallers for the org.jgroups.util package.
 * @author Paul Ferraro
 */
public enum JGroupsUtilMarshallerProvider implements ProtoStreamMarshallerProvider {
    UUID(new UUIDMarshaller()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    JGroupsUtilMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
