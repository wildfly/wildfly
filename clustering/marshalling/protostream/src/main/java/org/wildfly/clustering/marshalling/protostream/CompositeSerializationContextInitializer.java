/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.EnumSet;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;

/**
 * {@link SerializationContextInitializer} that registers a set of {@link SerializationContextInitializer} instances.
 * @author Paul Ferraro
 */
public class CompositeSerializationContextInitializer implements SerializationContextInitializer {

    private final Iterable<? extends SerializationContextInitializer> initializers;

    public CompositeSerializationContextInitializer(SerializationContextInitializer... initializers) {
        this(Arrays.asList(initializers));
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
