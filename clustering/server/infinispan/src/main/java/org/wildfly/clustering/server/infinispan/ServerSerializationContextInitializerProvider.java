/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
