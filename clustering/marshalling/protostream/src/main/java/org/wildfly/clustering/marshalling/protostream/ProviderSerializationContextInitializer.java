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

import static org.wildfly.clustering.marshalling.protostream.MarshallerProvider.ClassPredicate.ABSTRACT;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.infinispan.protostream.SerializationContext;

/**
 * A {@link org.infinispan.protostream.SerializationContextInitializer} that registers enumerated marshallers.
 * @author Paul Ferraro
 * @param <E> the marshaller provider provider type
 */
public class ProviderSerializationContextInitializer<E extends Enum<E> & ProtoStreamMarshallerProvider> extends AbstractSerializationContextInitializer {

    private final Class<E> providerClass;

    public ProviderSerializationContextInitializer(String resourceName, Class<E> providerClass) {
        super(resourceName);
        this.providerClass = providerClass;
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        Set<E> providers = EnumSet.allOf(this.providerClass);
        List<ProtoStreamMarshaller<?>> abstractMarshallers = new ArrayList<>(providers.size());
        for (E provider : providers) {
            ProtoStreamMarshaller<?> marshaller = provider.getMarshaller();
            // If marshaller type is abstract, register via a MarshallerProvider instead
            if (ABSTRACT.test(marshaller.getJavaClass())) {
                abstractMarshallers.add(marshaller);
            } else {
                context.registerMarshaller(marshaller);
            }
        }
        if (!abstractMarshallers.isEmpty()) {
            context.registerMarshallerProvider(new MarshallerProvider(ABSTRACT, abstractMarshallers));
        }
    }
}
