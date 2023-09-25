/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProviderSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializerProvider;
import org.wildfly.clustering.marshalling.protostream.ValueMarshaller;

/**
 * Provider of the {@link SerializationContextInitializer} instances for this module.
 * @author Paul Ferraro
 */
public enum SingletonSerializationContextInitializerProvider implements SerializationContextInitializerProvider {
    SERVICE(new ProviderSerializationContextInitializer<>("org.jboss.msc.service.proto", ServiceMarshallerProvider.class)),
    SINGLETON(new AbstractSerializationContextInitializer() {
        @Override
        public void registerMarshallers(SerializationContext context) {
            context.registerMarshaller(new SingletonElectionCommandMarshaller());
            context.registerMarshaller(new ValueMarshaller<>(new StartCommand()));
            context.registerMarshaller(new ValueMarshaller<>(new StopCommand()));
            context.registerMarshaller(new ValueMarshaller<>(new PrimaryProviderCommand()));
            context.registerMarshaller(new ValueMarshaller<>(new SingletonValueCommand<>()));
        }
    }),
    ;
    private final SerializationContextInitializer initializer;

    SingletonSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
