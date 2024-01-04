/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * @author Paul Ferraro
 */
public class ExternalizerTesterFactory implements MarshallingTesterFactory {

    private final Iterable<? extends Externalizer<?>> externalizers;

    public <E extends Enum<E> & Externalizer<Object>> ExternalizerTesterFactory(Class<E> enumClass) {
        this(EnumSet.allOf(enumClass));
    }

    public ExternalizerTesterFactory(Externalizer<?>... externalizers) {
        this(Arrays.asList(externalizers));
    }

    public ExternalizerTesterFactory(Iterable<? extends Externalizer<?>> externalizers) {
        this.externalizers = externalizers;
    }

    @Override
    public <T> MarshallingTester<T> createTester() {
        return new MarshallingTester<>(new ExternalizerMarshaller<T>(this.externalizers));
    }
}
