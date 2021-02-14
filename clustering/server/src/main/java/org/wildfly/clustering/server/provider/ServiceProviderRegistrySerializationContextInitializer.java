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

package org.wildfly.clustering.server.provider;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SimpleFieldSetMarshaller;
import org.wildfly.clustering.marshalling.protostream.ValueMarshaller;
import org.wildfly.clustering.server.group.InfinispanAddressMarshaller;

/**
 * {@link org.infinispan.protostream.SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
public class ServiceProviderRegistrySerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new ValueMarshaller<>(GetLocalServicesCommand::new));
        ProtoStreamMarshaller<Address> addressMarshaller = new SimpleFieldSetMarshaller<>(Address.class, InfinispanAddressMarshaller.INSTANCE);
        context.registerMarshaller(new FunctionalMarshaller<>(ConcurrentAddressSetAddFunction.class, addressMarshaller, ConcurrentAddressSetAddFunction::getOperand, ConcurrentAddressSetAddFunction::new));
        context.registerMarshaller(new FunctionalMarshaller<>(ConcurrentAddressSetRemoveFunction.class, addressMarshaller, ConcurrentAddressSetRemoveFunction::getOperand, ConcurrentAddressSetRemoveFunction::new));
        context.registerMarshaller(new FunctionalMarshaller<>(CopyOnWriteAddressSetAddFunction.class, addressMarshaller, CopyOnWriteAddressSetAddFunction::getOperand, CopyOnWriteAddressSetAddFunction::new));
        context.registerMarshaller(new FunctionalMarshaller<>(CopyOnWriteAddressSetRemoveFunction.class, addressMarshaller, CopyOnWriteAddressSetRemoveFunction::getOperand, CopyOnWriteAddressSetRemoveFunction::new));
    }
}
