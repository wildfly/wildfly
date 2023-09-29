/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.provider;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;

/**
 * {@link org.infinispan.protostream.SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
public class ServiceProviderRegistrySerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new AddressSetFunctionMarshaller<>(AddressSetAddFunction.class, AddressSetAddFunction::new));
        context.registerMarshaller(new AddressSetFunctionMarshaller<>(AddressSetRemoveFunction.class, AddressSetRemoveFunction::new));
    }
}
