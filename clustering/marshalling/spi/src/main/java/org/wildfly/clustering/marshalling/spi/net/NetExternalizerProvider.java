/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.ExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.StringExternalizer;

/**
 * Externalizers for the java.net package.
 * @author Paul Ferraro
 */
public enum NetExternalizerProvider implements ExternalizerProvider {

    INET_ADDRESS(new InetAddressExternalizer<>(InetAddress.class, OptionalInt.empty())),
    INET4_ADDRESS(new InetAddressExternalizer<>(Inet4Address.class, OptionalInt.of(4))),
    INET6_ADDRESS(new InetAddressExternalizer<>(Inet6Address.class, OptionalInt.of(16))),
    INET_SOCKET_ADDRESS(new InetSocketAddressExternalizer()),
    URI(new StringExternalizer<>(java.net.URI.class, java.net.URI::create, java.net.URI::toString)),
    URL(new URLExternalizer()),
    ;
    private final Externalizer<?> externalizer;

    NetExternalizerProvider(Externalizer<?> externalizer) {
        this.externalizer = externalizer;
    }

    @Override
    public Externalizer<?> getExternalizer() {
        return this.externalizer;
    }
}
