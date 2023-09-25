/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.util.EnumSet;

import org.junit.Assert;

/**
 * Validates marshalling of an enum.
 * @author Paul Ferraro
 */
public class EnumMarshallingTester<E extends Enum<E>> {

    private final Class<E> enumClass;
    private final MarshallingTester<E> tester;

    public EnumMarshallingTester(Class<E> enumClass, MarshallingTester<E> tester) {
        this.enumClass = enumClass;
        this.tester = tester;
    }

    public void test() throws IOException {
        for (E value : EnumSet.allOf(this.enumClass)) {
            this.tester.test(value, Assert::assertSame);
        }
    }
}
