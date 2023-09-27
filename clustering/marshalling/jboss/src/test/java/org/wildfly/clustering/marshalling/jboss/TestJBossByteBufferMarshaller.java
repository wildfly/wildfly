/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.jboss;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.OptionalInt;

import org.jboss.marshalling.MarshallingConfiguration;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;

/**
 * @author Paul Ferraro
 */
public enum TestJBossByteBufferMarshaller implements MarshallingConfigurationRepository, ByteBufferMarshaller {
    INSTANCE;

    private final MarshallingConfiguration configuration;
    private final ByteBufferMarshaller marshaller;

    TestJBossByteBufferMarshaller() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        this.configuration = new MarshallingConfiguration();
        this.configuration.setClassTable(new DynamicClassTable(loader));
        this.configuration.setObjectTable(new DynamicExternalizerObjectTable(loader));
        this.marshaller = new JBossByteBufferMarshaller(this, loader);
    }

    @Override
    public boolean isMarshallable(Object object) {
        return this.marshaller.isMarshallable(object);
    }

    @Override
    public Object readFrom(InputStream input) throws IOException {
        return this.marshaller.readFrom(input);
    }

    @Override
    public void writeTo(OutputStream output, Object object) throws IOException {
        this.marshaller.writeTo(output, object);
    }

    @Override
    public OptionalInt size(Object object) {
        return this.marshaller.size(object);
    }

    @Override
    public int getCurrentMarshallingVersion() {
        return 0;
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration(int version) {
        return this.configuration;
    }
}
