/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.util.UtilMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.util.concurrent.ConcurrentMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.util.concurrent.atomic.AtomicMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.math.MathMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.net.NetMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.sql.SQLMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.time.TimeMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum DefaultSerializationContextInitializerProvider implements SerializationContextInitializerProvider {
    ANY(new AnySerializationContextInitializer()),
    MATH(new ProviderSerializationContextInitializer<>("java.math.proto", MathMarshallerProvider.class)),
    NET(new ProviderSerializationContextInitializer<>("java.net.proto", NetMarshallerProvider.class)),
    SQL(new ProviderSerializationContextInitializer<>("java.sql.proto", SQLMarshallerProvider.class)),
    TIME(new ProviderSerializationContextInitializer<>("java.time.proto", TimeMarshallerProvider.class)),
    UTIL(new ProviderSerializationContextInitializer<>("java.util.proto", UtilMarshallerProvider.class)),
    ATOMIC(new ProviderSerializationContextInitializer<>("java.util.concurrent.atomic.proto", AtomicMarshallerProvider.class)),
    CONCURRENT(new ProviderSerializationContextInitializer<>("java.util.concurrent.proto", ConcurrentMarshallerProvider.class)),
    MARSHALLING(new ProviderSerializationContextInitializer<>("org.wildfly.clustering.marshalling.spi.proto", MarshallingMarshallerProvider.class)),
    ;
    private final SerializationContextInitializer initializer;

    DefaultSerializationContextInitializerProvider(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public SerializationContextInitializer getInitializer() {
        return this.initializer;
    }
}
