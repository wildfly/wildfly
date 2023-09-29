/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan.provider;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleFieldSetMarshaller;
import org.wildfly.clustering.server.infinispan.group.InfinispanAddressMarshaller;

/**
 * {@link org.infinispan.protostream.SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
public class ServiceProviderRegistrySerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new AddressSetFunctionMarshaller<>(AddressSetAddFunction.class, AddressSetAddFunction::new));
        context.registerMarshaller(new AddressSetFunctionMarshaller<>(AddressSetRemoveFunction.class, AddressSetRemoveFunction::new));
        // Deprecated functions to be removed
        ProtoStreamMarshaller<Address> addressMarshaller = new SimpleFieldSetMarshaller<>(Address.class, InfinispanAddressMarshaller.INSTANCE);
        context.registerMarshaller(new FunctionalMarshaller<>(ConcurrentAddressSetAddFunction.class, addressMarshaller, ConcurrentAddressSetAddFunction::getValue, ConcurrentAddressSetAddFunction::new));
        context.registerMarshaller(new FunctionalMarshaller<>(ConcurrentAddressSetRemoveFunction.class, addressMarshaller, ConcurrentAddressSetRemoveFunction::getValue, ConcurrentAddressSetRemoveFunction::new));
        context.registerMarshaller(new FunctionalMarshaller<>(CopyOnWriteAddressSetAddFunction.class, addressMarshaller, CopyOnWriteAddressSetAddFunction::getValue, CopyOnWriteAddressSetAddFunction::new));
        context.registerMarshaller(new FunctionalMarshaller<>(CopyOnWriteAddressSetRemoveFunction.class, addressMarshaller, CopyOnWriteAddressSetRemoveFunction::getValue, CopyOnWriteAddressSetRemoveFunction::new));
    }
}
