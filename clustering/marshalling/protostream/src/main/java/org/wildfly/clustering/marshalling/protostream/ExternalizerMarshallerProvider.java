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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import org.infinispan.protostream.BaseMarshaller;
import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Provider for externalizer-based marshallers.
 * @author Paul Ferraro
 */
@Deprecated
public class ExternalizerMarshallerProvider extends MarshallerProvider {

    private static final Function<Externalizer<?>, BaseMarshaller<?>> MAPPER = new Function<Externalizer<?>, BaseMarshaller<?>>() {
        @Override
        public BaseMarshaller<?> apply(Externalizer<?> externalizer) {
            Class<?> targetClass = externalizer.getTargetClass();
            return targetClass.isEnum() ? new EnumMarshaller<>(targetClass.asSubclass(Enum.class)) : new ExternalizerMarshaller<>(externalizer);
        }
    };

    public ExternalizerMarshallerProvider(Externalizer<?>... externalizers) {
        this(Arrays.asList(externalizers));
    }

    public ExternalizerMarshallerProvider(Collection<? extends Externalizer<?>> externalizers) {
        super(ClassPredicate.ABSTRACT, externalizers.stream().map(MAPPER));
    }
}
