/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        tester.test(new AtomicBoolean(false), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
        tester.test(new AtomicBoolean(true), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
    }

    @Test
    public void testAtomicInteger() throws IOException {
        MarshallingTester<AtomicInteger> tester = this.factory.createTester();
        tester.test(new AtomicInteger(), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
        tester.test(new AtomicInteger(Byte.MAX_VALUE), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
        tester.test(new AtomicInteger(Integer.MAX_VALUE), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
    }

    @Test
    public void testAtomicLong() throws IOException {
        MarshallingTester<AtomicLong> tester = this.factory.createTester();
        tester.test(new AtomicLong(), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
        tester.test(new AtomicLong(Short.MAX_VALUE), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
        tester.test(new AtomicLong(Long.MAX_VALUE), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
    }

    @Test
    public void testAtomicReference() throws IOException {
        MarshallingTester<AtomicReference<Object>> tester = this.factory.createTester();
        tester.test(new AtomicReference<>(), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
        tester.test(new AtomicReference<>(Boolean.TRUE), (expected, actual) -> Assert.assertEquals(expected.get(), actual.get()));
    }
}
