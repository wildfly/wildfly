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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

/**
 * Generic tests for java.util.concurrent.atomic.* classes.
 * @author Paul Ferraro
 */
public abstract class AbstractAtomicTestCase {

    private final MarshallingTesterFactory factory;

    public AbstractAtomicTestCase(MarshallingTesterFactory factory) {
        this.factory = factory;
    }

    @Test
    public void testAtomicBoolean() throws IOException {
        MarshallingTester<AtomicBoolean> tester = this.factory.createTester();
        tester.test(new AtomicBoolean(true), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
    }

    @Test
    public void testAtomicInteger() throws IOException {
        MarshallingTester<AtomicInteger> tester = this.factory.createTester();
        tester.test(new AtomicInteger(Integer.MAX_VALUE), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
    }

    @Test
    public void testAtomicLong() throws IOException {
        MarshallingTester<AtomicLong> tester = this.factory.createTester();
        tester.test(new AtomicLong(Integer.MAX_VALUE), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
    }

    @Test
    public void testAtomicReference() throws IOException {
        MarshallingTester<AtomicReference<Object>> tester = this.factory.createTester();
        tester.test(new AtomicReference<>(Boolean.TRUE), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
    }
}
