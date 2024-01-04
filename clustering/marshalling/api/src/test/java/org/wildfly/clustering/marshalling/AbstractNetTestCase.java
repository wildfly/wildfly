/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
