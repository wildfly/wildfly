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

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;

import org.junit.Test;

/**
 * Generic tests for java.net.* classes.
 * @author Paul Ferraro
 */
public abstract class AbstractNetTestCase {

    private final MarshallingTesterFactory factory;

    public AbstractNetTestCase(MarshallingTesterFactory factory) {
        this.factory = factory;
    }

    @Test
    public void testURI() throws IOException {
        MarshallingTester<URI> tester = this.factory.createTester();
        tester.test(URI.create("http://wildfly.org/news/"));
    }

    @Test
    public void testURL() throws IOException {
        MarshallingTester<URL> tester = this.factory.createTester();
        tester.test(new URL("http://wildfly.org/news/"));
    }

    @Test
    public void testInetAddress() throws IOException {
        MarshallingTester<InetAddress> tester = this.factory.createTester();
        tester.test(InetAddress.getLoopbackAddress());
        tester.test(InetAddress.getLocalHost());
        tester.test(InetAddress.getByName("127.0.0.1"));
        tester.test(InetAddress.getByName("::1"));
        tester.test(InetAddress.getByName("0.0.0.0"));
        tester.test(InetAddress.getByName("::"));
    }

    @Test
    public void testInetSocketAddress() throws IOException {
        MarshallingTester<InetSocketAddress> tester = this.factory.createTester();
        tester.test(InetSocketAddress.createUnresolved("foo.bar", 0));
        tester.test(InetSocketAddress.createUnresolved("foo.bar", Short.MAX_VALUE));
        tester.test(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        tester.test(new InetSocketAddress(InetAddress.getLoopbackAddress(), Short.MAX_VALUE));
        tester.test(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        tester.test(new InetSocketAddress(InetAddress.getLocalHost(), Short.MAX_VALUE));
        tester.test(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0));
        tester.test(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), Short.MAX_VALUE));
        tester.test(new InetSocketAddress(InetAddress.getByName("::"), 0));
        tester.test(new InetSocketAddress(InetAddress.getByName("::"), Short.MAX_VALUE));
    }
}
