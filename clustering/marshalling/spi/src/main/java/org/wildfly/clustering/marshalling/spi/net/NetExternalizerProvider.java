/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
