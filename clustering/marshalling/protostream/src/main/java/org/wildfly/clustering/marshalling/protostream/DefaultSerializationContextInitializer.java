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

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.util.UtilMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.util.concurrent.ConcurrentMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.util.concurrent.atomic.AtomicMarshallerProvider;
import org.wildfly.clustering.marshalling.spi.MarshallingExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.net.NetExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.sql.SQLExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.time.TimeExternalizerProvider;

/**
 * @author Paul Ferraro
 */
public enum DefaultSerializationContextInitializer implements SerializationContextInitializer {
    ANY(new AnySerializationContextInitializer()),
    NET(new ExternalizerSerializationContextInitializer<>("java.net.proto", NetExternalizerProvider.class)),
    SQL(new ExternalizerSerializationContextInitializer<>("java.sql.proto", SQLExternalizerProvider.class)),
    TIME(new ExternalizerSerializationContextInitializer<>("java.time.proto", TimeExternalizerProvider.class)),
    UTIL(new ProviderSerializationContextInitializer<>("java.util.proto", UtilMarshallerProvider.class)),
    ATOMIC(new ProviderSerializationContextInitializer<>("java.util.concurrent.atomic.proto", AtomicMarshallerProvider.class)),
    CONCURRENT(new ProviderSerializationContextInitializer<>("java.util.concurrent.proto", ConcurrentMarshallerProvider.class)),
    MARSHALLING(new ExternalizerSerializationContextInitializer<>("org.wildfly.clustering.marshalling.spi.proto", MarshallingExternalizerProvider.class)),
    ;
    private final SerializationContextInitializer initializer;

    DefaultSerializationContextInitializer(SerializationContextInitializer initializer) {
        this.initializer = initializer;
    }

    @Deprecated
    @Override
    public String getProtoFileName() {
        return this.initializer.getProtoFileName();
    }

    @Deprecated
    @Override
    public String getProtoFile() {
        return this.initializer.getProtoFile();
    }

    @Override
    public void registerSchema(SerializationContext context) {
        this.initializer.registerSchema(context);
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        this.initializer.registerMarshallers(context);
    }
}
