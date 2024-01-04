/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

/**
 * Factory for creating marshalling testers.
 * @author Paul Ferraro
 */
public interface MarshallingTesterFactory {
    <T> MarshallingTester<T> createTester();

    default <E extends Enum<E>> EnumMarshallingTester<E> createTester(Class<E> enumClass) {
        return new EnumMarshallingTester<>(enumClass, this.createTester());
    }
}
