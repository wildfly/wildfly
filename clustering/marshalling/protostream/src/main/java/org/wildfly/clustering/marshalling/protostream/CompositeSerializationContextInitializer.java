/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.List;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;

/**
 * {@link SerializationContextInitializer} that registers a set of {@link SerializationContextInitializer} instances.
 * @author Paul Ferraro
 */
public class CompositeSerializationContextInitializer implements SerializationContextInitializer {

    private final Iterable<? extends SerializationContextInitializer> initializers;

    public CompositeSerializationContextInitializer(SerializationContextInitializer initializer1, SerializationContextInitializer initializer2) {
        this(List.of(initializer1, initializer2));
    }

    public CompositeSerializationContextInitializer(SerializationContextInitializer... initializers) {
        this(List.of(initializers));
    }

    public <E extends Enum<E> & SerializationContextInitializer> CompositeSerializationContextInitializer(Class<E> enumClass) {
        this(EnumSet.allOf(enumClass));
    }

    public CompositeSerializationContextInitializer(Iterable<? extends SerializationContextInitializer> initializers) {
        this.initializers = initializers;
    }

    @Deprecated
    @Override
    public String getProtoFileName() {
        return null;
    }

    @Deprecated
    @Override
    public String getProtoFile() throws UncheckedIOException {
        return null;
    }

    @Override
    public void registerSchema(SerializationContext context) {
        for (SerializationContextInitializer initializer : this.initializers) {
            initializer.registerSchema(context);
        }
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        for (SerializationContextInitializer initializer : this.initializers) {
            initializer.registerMarshallers(context);
        }
    }
}
