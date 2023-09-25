/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.util.function.BiConsumer;

import org.junit.Assert;

/**
 * Generic interface for various marshalling testers.
 * @author Paul Ferraro
 */
public interface Tester<T> {

    default void test(T subject) throws IOException {
        this.test(subject, Assert::assertEquals);
    }

    /**
     * Same as {@link #test(Object)}, but additionally validates equality of hash code.
     * @param subject a test subject
     * @throws IOException if marshalling of the test subject fails
     */
    default void testKey(T subject) throws IOException {
        this.test(subject, (value1, value2) -> {
            Assert.assertEquals(value1, value2);
            Assert.assertEquals(value1.hashCode(), value2.hashCode());
        });
    }

    void test(T subject, BiConsumer<T, T> assertion) throws IOException;
}
