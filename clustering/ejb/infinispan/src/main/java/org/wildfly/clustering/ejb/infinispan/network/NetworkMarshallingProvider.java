/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.network;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * Provides marshallers for the <code>org.jboss.as.network</code> package.
 * @author Paul Ferraro
 */
public enum NetworkMarshallingProvider implements ProtoStreamMarshallerProvider {

    CLIENT_MAPPING(new ClientMappingMarshaller()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    NetworkMarshallingProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
