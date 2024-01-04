/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
