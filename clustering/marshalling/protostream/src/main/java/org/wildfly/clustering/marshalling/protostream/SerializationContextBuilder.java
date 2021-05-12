/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.marshalling.protostream;

import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.SerializationContextImpl;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Builds a ProtoStream {@link ImmutableSerializationContext}.
 * @author Paul Ferraro
 */
public class SerializationContextBuilder {
    private static final String PROTOSTREAM_BASE_PACKAGE_NAME = org.infinispan.protostream.BaseMarshaller.class.getPackage().getName();

    private final SerializationContext context = new SerializationContextImpl(Configuration.builder().build());

    /**
     * Constructs a builder for a {@link SerializationContext} using a default set of initializers.
     * @param marshaller a class loader marshaller
     */
    public SerializationContextBuilder(ClassLoaderMarshaller marshaller) {
        // Load default schemas first, so they can be referenced by loader-specific schemas
        this.register(Collections.singleton(new LangSerializationContextInitializer(marshaller)));
        this.register(EnumSet.allOf(DefaultSerializationContextInitializerProvider.class));
    }

    /**
     * Returns an immutable {@link SerializationContext}.
     * @return the completed and immutable serialization context
     */
    public ImmutableSerializationContext build() {
        return this.context;
    }

    /**
     * Registers an initializer with the {@link SerializationContext}.
     * @param initializer an initializer for the {@link SerializationContext}.
     * @return this builder
     */
    public SerializationContextBuilder register(SerializationContextInitializer initializer) {
        this.init(initializer);
        return this;
    }

    /**
     * Registers a number of initializers with the {@link SerializationContext}.
     * @param initializers one or more initializers for the {@link SerializationContext}.
     * @return this builder
     */
    public SerializationContextBuilder register(SerializationContextInitializer... initializers) {
        return this.register(Arrays.asList(initializers));
    }

    /**
     * Registers a number of initializers with the {@link SerializationContext}.
     * @param initializers one or more initializers for the {@link SerializationContext}.
     * @return this builder
     */
    public SerializationContextBuilder register(Iterable<? extends SerializationContextInitializer> initializers) {
        for (SerializationContextInitializer initializer : initializers) {
            this.init(initializer);
        }
        return this;
    }

    /**
     * Loads {@link SerializationContextInitializer} instances from the specified {@link ClassLoader} and registers then with the {@link SerializationContext}.
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
        PrivilegedAction<Iterator<SerializationContextInitializer>> action = new PrivilegedAction<Iterator<SerializationContextInitializer>>() {
            @Override
            public Iterator<SerializationContextInitializer> run() {
                return ServiceLoader.load(SerializationContextInitializer.class, loader).iterator();
            }
        };
        Iterator<SerializationContextInitializer> initializers = WildFlySecurityManager.doUnchecked(action);
        boolean init = initializers.hasNext();
        while (initializers.hasNext()) {
            SerializationContextInitializer initializer = initializers.next();
            // Do not load initializers from protostream-types
            if (!initializer.getClass().getName().startsWith(PROTOSTREAM_BASE_PACKAGE_NAME)) {
                this.init(initializer);
            }
        }
        return init;
    }

    private void init(SerializationContextInitializer initializer) {
        initializer.registerSchema(this.context);
        initializer.registerMarshallers(this.context);
    }
}
