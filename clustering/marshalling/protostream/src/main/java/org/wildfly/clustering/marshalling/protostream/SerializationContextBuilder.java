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

    private final SerializationContext context = new SerializationContextImpl(Configuration.builder().build());

    public SerializationContextBuilder() {
        // Load default schemas first, so they can be referenced by loader-specific schemas
        this.register(EnumSet.allOf(DefaultSerializationContextInitializer.class));
    }

    public ImmutableSerializationContext build() {
        return this.context;
    }

    public SerializationContextBuilder register(SerializationContextInitializer initializer) {
        this.init(initializer);
        return this;
    }

    public SerializationContextBuilder register(SerializationContextInitializer... initializers) {
        return this.register(Arrays.asList(initializers));
    }

    public SerializationContextBuilder register(Iterable<? extends SerializationContextInitializer> initializers) {
        this.init(initializers);
        return this;
    }

    public SerializationContextBuilder register(ClassLoader... loaders) throws NoSuchElementException {
        PrivilegedAction<Boolean> action = new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                boolean init = false;
                for (ClassLoader loader : loaders) {
                    init |= SerializationContextBuilder.this.init(ServiceLoader.load(SerializationContextInitializer.class, loader));
                }
                return init;
            }
        };
        if (!WildFlySecurityManager.doUnchecked(action).booleanValue()) {
            throw new NoSuchElementException();
        }
        return this;
    }

    boolean init(Iterable<? extends SerializationContextInitializer> initializers) {
        Iterator<? extends SerializationContextInitializer> iter = initializers.iterator();
        boolean result = iter.hasNext();
        while (iter.hasNext()) {
            this.init(iter.next());
        }
        return result;
    }

    private void init(SerializationContextInitializer initializer) {
        initializer.registerSchema(this.context);
        initializer.registerMarshallers(this.context);
    }
}
