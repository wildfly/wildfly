/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.IOException;

import org.wildfly.common.function.ExceptionFunction;

/**
 * Functional marshaller whose marshalled type is a subclass of the mapped marshaller.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 * @param <V> the type of the mapped marshaller
 */
public class SimpleFunctionalMarshaller<T extends V, V> extends FunctionalMarshaller<T, V> {

    public SimpleFunctionalMarshaller(Class<T> targetClass, ProtoStreamMarshaller<V> marshaller, ExceptionFunction<V, T, IOException> factory) {
        super(targetClass, marshaller, value -> value, factory);
    }
}
