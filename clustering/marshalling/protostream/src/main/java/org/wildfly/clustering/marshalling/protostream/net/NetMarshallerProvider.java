/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.net;

import java.net.InetAddress;

import org.wildfly.clustering.marshalling.protostream.FieldSetProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * Provider for java.net marshallers.
 * @author Paul Ferraro
 */
public enum NetMarshallerProvider implements ProtoStreamMarshallerProvider {

    INET_ADDRESS(new FieldSetProtoStreamMarshaller<>(InetAddress.class, InetAddressMarshaller.INSTANCE)),
    INET_SOCKET_ADDRESS(new InetSocketAddressMarshaller()),
    URI(new URIMarshaller()),
    URL(new URLMarshaller()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    NetMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
