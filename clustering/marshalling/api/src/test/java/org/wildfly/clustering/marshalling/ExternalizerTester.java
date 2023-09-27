/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling;

/**
 * A marshalling tester for a single externalizer.
 * @author Paul Ferraro
 */
public class ExternalizerTester<T> extends MarshallingTester<T> {

    public ExternalizerTester(Externalizer<T> externalizer) {
        super(new ExternalizerMarshaller<>(externalizer));
    }
}
