/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.reflect;

import java.io.IOException;
import java.util.function.UnaryOperator;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * A decorator marshaller that writes the decorated object while holding its monitor lock.
 * e.g. to enable iteration over a decorated collection without the risk of a ConcurrentModificationException.
 * @author Paul Ferraro
 */
public class SynchronizedDecoratorMarshaller<T> extends DecoratorMarshaller<T> {

    /**
     * Constructs a decorator marshaller.
     * @param decoratedClass the generalized type of the decorated object
     * @param decorator the decoration function
     * @param sample a sample object used to determine the type of the decorated object
     */
    public SynchronizedDecoratorMarshaller(Class<T> decoratedClass, UnaryOperator<T> decorator, T sample) {
        super(decoratedClass, decorator, sample);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, T value) throws IOException {
        synchronized (value) {
            super.writeTo(writer, value);
        }
    }
}
