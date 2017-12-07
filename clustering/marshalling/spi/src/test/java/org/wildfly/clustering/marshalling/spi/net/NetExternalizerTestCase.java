/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;

import org.junit.Test;
import org.wildfly.clustering.marshalling.ExternalizerTester;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;

/**
 * Unit test for {@link URIExternalizer}.
 * @author Paul Ferraro
 */
public class NetExternalizerTestCase {

    @Test
    public void test() throws ClassNotFoundException, IOException {
        new ExternalizerTester<>(DefaultExternalizer.URI.cast(URI.class)).test(URI.create("http://wildfly.org/news/"));
        new ExternalizerTester<>(DefaultExternalizer.URL.cast(URL.class)).test(new URL("http://wildfly.org/news/"));

        new ExternalizerTester<>(DefaultExternalizer.INET_ADDRESS.cast(InetAddress.class)).test(InetAddress.getLoopbackAddress());
        new ExternalizerTester<>(DefaultExternalizer.INET4_ADDRESS.cast(InetAddress.class)).test(InetAddress.getByName("127.0.0.1"));
        new ExternalizerTester<>(DefaultExternalizer.INET6_ADDRESS.cast(InetAddress.class)).test(InetAddress.getByName("::1"));
        new ExternalizerTester<>(DefaultExternalizer.INET_SOCKET_ADDRESS.cast(InetSocketAddress.class)).test(InetSocketAddress.createUnresolved("hostname", 0));
        new ExternalizerTester<>(DefaultExternalizer.INET_SOCKET_ADDRESS.cast(InetSocketAddress.class)).test(new InetSocketAddress(InetAddress.getLoopbackAddress(), Short.MAX_VALUE));
    }
}
