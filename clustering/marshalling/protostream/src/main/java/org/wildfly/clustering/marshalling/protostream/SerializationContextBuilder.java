/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Builds a ProtoStream {@link ImmutableSerializationContext}.
 * @author Paul Ferraro
 */
public class SerializationContextBuilder {
    private static final String PROTOSTREAM_BASE_PACKAGE_NAME = org.infinispan.protostream.BaseMarshaller.class.getPackage().getName();

    private final DefaultSerializationContext context = new DefaultSerializationContext();

    /**
     * Constructs a builder for a {@link org.infinispan.protostream.SerializationContext} using a default set of initializers.
     * @param marshaller a class loader marshaller
     */
    public SerializationContextBuilder(ClassLoaderMarshaller marshaller) {
        // Load default schemas first, so they can be referenced by loader-specific schemas
        this.register(Collections.singleton(new LangSerializationContextInitializer(marshaller)));
        this.register(EnumSet.allOf(DefaultSerializationContextInitializerProvider.class));
    }

    /**
     * Returns an immutable {@link org.infinispan.protostream.SerializationContext}.
     * @return the completed and immutable serialization context
     */
    public ImmutableSerializationContext build() {
        return this.context.get();
    }

    /**
     * Registers an initializer with the {@link org.infinispan.protostream.SerializationContext}.
     * @param initializer an initializer for the {@link org.infinispan.protostream.SerializationContext}.
     * @return this builder
     */
    public SerializationContextBuilder register(SerializationContextInitializer initializer) {
        this.init(initializer);
        return this;
    }

    /**
     * Registers a number of initializers with the {@link org.infinispan.protostream.SerializationContext}.
     * @param initializers one or more initializers for the {@link org.infinispan.protostream.SerializationContext}.
     * @return this builder
     */
    public SerializationContextBuilder register(Iterable<? extends SerializationContextInitializer> initializers) {
        for (SerializationContextInitializer initializer : initializers) {
            this.init(initializer);
        }
        return this;
    }

    /**
     * Loads {@link SerializationContextInitializer} instances from the specified {@link ClassLoader} and registers then with the {@link org.infinispan.protostream.SerializationContext}.
     * @param loader a class loader
     * @return this builder
     */
    public SerializationContextBuilder load(ClassLoader loader) {
        this.tryLoad(loader);
        return this;
    }

    /**
     * Similar to {@link #load(ClassLoader)}, but throws a {@link NoSuchElementException} if no {@link SerializationContextInitializer} instances were found.
     * @param loader a class loader
     * @return this builder
     */
    public SerializationContextBuilder require(ClassLoader loader) {
        if (!this.tryLoad(loader)) {
            throw new NoSuchElementException();
        }
        return this;
    }

    private boolean tryLoad(ClassLoader loader) {
        PrivilegedAction<Boolean> action = new PrivilegedAction<>() {
            @Override
            public Boolean run() {
                Iterator<SerializationContextInitializer> initializers = ServiceLoader.load(SerializationContextInitializer.class, loader).iterator();
                boolean init = false;
                while (initializers.hasNext()) {
                    SerializationContextInitializer initializer = initializers.next();
                    // Do not load initializers from protostream-types
                    if (!initializer.getClass().getName().startsWith(PROTOSTREAM_BASE_PACKAGE_NAME)) {
                        SerializationContextBuilder.this.init(initializer);
                        init = true;
                    }
                }
                return init;
            }
        };
        return WildFlySecurityManager.doUnchecked(action);
    }

    void init(SerializationContextInitializer initializer) {
        initializer.registerSchema(this.context);
        initializer.registerMarshallers(this.context);
    }
}
