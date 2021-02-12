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

package org.wildfly.clustering.marshalling.jboss;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
        this.configuration.setObjectTable(new ExternalizerObjectTable(loader));
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
    public int getCurrentMarshallingVersion() {
        return 0;
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration(int version) {
        return this.configuration;
    }
}
