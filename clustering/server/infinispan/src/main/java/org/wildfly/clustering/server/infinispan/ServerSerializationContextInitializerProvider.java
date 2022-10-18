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

package org.wildfly.clustering.server.infinispan;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProviderSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;
import org.wildfly.clustering.server.infinispan.dispatcher.CommandDispatcherSerializationContextInitializer;
import org.wildfly.clustering.server.infinispan.group.GroupSerializationContextInitializer;
import org.wildfly.clustering.server.infinispan.group.InfinispanJGroupsTransportMarshallerProvider;
import org.wildfly.clustering.server.infinispan.group.InfinispanTransportMarshallerProvider;
import org.wildfly.clustering.server.infinispan.group.JGroupsStackMarshallerProvider;
import org.wildfly.clustering.server.infinispan.group.JGroupsUtilMarshallerProvider;
import org.wildfly.clustering.server.infinispan.provider.ServiceProviderRegistrySerializationContextInitializer;

/**
 * Provider of the {@link SerializationContextInitializer} instances for this module.
 * @author Paul Ferraro
 */
public enum ServerSerializationContextInitializerProvider implements SerializationContextInitializerProvider {
    COMMAND_DISPATCHER(new CommandDispatcherSerializationContextInitializer()),
    JGROUPS_UTIL(new ProviderSerializationContextInitializer<>("org.jgroups.util.proto", JGroupsUtilMarshallerProvider.class)),
    JGROUPS_STACK(new ProviderSerializationContextInitializer<>("org.jgroups.stack.proto", JGroupsStackMarshallerProvider.class)),
    INFINISPAN_TRANSPORT(new ProviderSerializationContextInitializer<>("org.infinispan.remoting.transport.proto", InfinispanTransportMarshallerProvider.class)),
    INFINISPAN_JGROUPS_TRANSPORT(new ProviderSerializationContextInitializer<>("org.infinispan.remoting.transport.jgroups.proto", InfinispanJGroupsTransportMarshallerProvider.class)),
    GROUP(new GroupSerializationContextInitializer()),
    PROVIDER(new ServiceProviderRegistrySerializationContextInitializer()),
    ;
    private final SerializationContextInitializer initializer;

    ServerSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
