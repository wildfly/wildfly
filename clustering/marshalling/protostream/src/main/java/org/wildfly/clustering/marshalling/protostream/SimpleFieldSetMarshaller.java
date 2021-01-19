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

import java.util.function.Function;
import java.util.function.Supplier;

import org.wildfly.common.function.Functions;

/**
 * Marshaller for an object whose fields are marshalled via a {@link FieldMarshaller}, but whose type is sufficiently simple to not require a builder.
 * @author Paul Ferraro
 * @param <T> the type of this marshaller
 */
public class SimpleFieldSetMarshaller<T> extends FieldSetMarshaller<T, T> {

    public SimpleFieldSetMarshaller(Class<? extends T> targetClass, FieldMarshaller<T, T> marshaller) {
        this(targetClass, marshaller, Functions.constantSupplier(null));
    }

    @SuppressWarnings("unchecked")
    public SimpleFieldSetMarshaller(FieldMarshaller<T, T> marshaller, Supplier<T> builderFactory) {
        this((Class<T>) builderFactory.get().getClass(), marshaller, builderFactory);
    }

    public SimpleFieldSetMarshaller(Class<? extends T> targetClass, FieldMarshaller<T, T> marshaller, Supplier<T> builderFactory) {
        super(targetClass, marshaller, builderFactory, Function.identity());
    }
}
