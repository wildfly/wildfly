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

import java.util.EnumSet;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link org.infinispan.protostream.SerializationContextInitializer} that registers enumerated marshallers.
 * @author Paul Ferraro
 * @param <E> the marshaller provider provider type
 */
public class ProviderSerializationContextInitializer<E extends Enum<E> & ProtoStreamMarshallerProvider> extends AbstractSerializationContextInitializer {

    private final Class<E> providerClass;

    public ProviderSerializationContextInitializer(String resourceName, Class<E> providerClass) {
        super(resourceName, WildFlySecurityManager.getClassLoaderPrivileged(providerClass));
        this.providerClass = providerClass;
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        for (E provider : EnumSet.allOf(this.providerClass)) {
            context.registerMarshaller(provider.getMarshaller());
        }
    }
}
