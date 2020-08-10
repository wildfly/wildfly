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

package org.wildfly.clustering.marshalling.jboss;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import org.jboss.marshalling.ClassExternalizerFactory;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ObjectTable;
import org.jboss.marshalling.SerializabilityChecker;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.marshalling.SimpleDataOutput;
import org.jboss.marshalling.Unmarshaller;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link ByteBufferMarshaller} based on JBoss Marshalling.
 * @author Paul Ferraro
 */
public class JBossByteBufferMarshaller implements ByteBufferMarshaller {

    private final MarshallerFactory factory = Marshalling.getMarshallerFactory("river", Marshalling.class.getClassLoader());
    private final MarshallingConfigurationRepository repository;
    private final WeakReference<ClassLoader> loader;

    public JBossByteBufferMarshaller(MarshallingConfigurationRepository repository, ClassLoader loader) {
        this.repository = repository;
        this.loader = new WeakReference<>(loader);
    }

    private MarshallingConfiguration getMarshallingConfiguration(int version) {
        return this.repository.getMarshallingConfiguration(version);
    }

    @Override
    public Object readFrom(InputStream input) throws IOException {
        try (SimpleDataInput data = new SimpleDataInput(Marshalling.createByteInput(input))) {
            int version = IndexSerializer.UNSIGNED_BYTE.readInt(data);
            ClassLoader loader = setThreadContextClassLoader(this.loader.get());
            try (Unmarshaller unmarshaller = this.factory.createUnmarshaller(this.repository.getMarshallingConfiguration(version))) {
                unmarshaller.start(data);
                Object result = unmarshaller.readObject();
                unmarshaller.finish();
                return result;
            } catch (ClassNotFoundException e) {
                InvalidClassException exception = new InvalidClassException(e.getMessage());
                exception.initCause(e);
                throw exception;
            } finally {
                setThreadContextClassLoader(loader);
            }
        }
    }

    @Override
    public void writeTo(OutputStream output, Object value) throws IOException {
        int version = this.repository.getCurrentMarshallingVersion();
        try (SimpleDataOutput data = new SimpleDataOutput(Marshalling.createByteOutput(output))) {
            IndexSerializer.UNSIGNED_BYTE.writeInt(data, version);
            ClassLoader loader = setThreadContextClassLoader(this.loader.get());
            try (Marshaller marshaller = this.factory.createMarshaller(this.getMarshallingConfiguration(version))) {
                marshaller.start(data);
                marshaller.writeObject(value);
                marshaller.finish();
            } finally {
                setThreadContextClassLoader(loader);
            }
        }
    }

    @Override
    public boolean isMarshallable(Object object) {
        if (object == null) return true;
        MarshallingConfiguration configuration = this.repository.getMarshallingConfiguration(this.repository.getCurrentMarshallingVersion());
        try {
            ObjectTable table = configuration.getObjectTable();
            if ((table != null) && table.getObjectWriter(object) != null) return true;
            ClassExternalizerFactory factory = configuration.getClassExternalizerFactory();
            if ((factory != null) && (factory.getExternalizer(object.getClass()) != null)) return true;
            SerializabilityChecker checker = configuration.getSerializabilityChecker();
            return ((checker == null) ? SerializabilityChecker.DEFAULT : checker).isSerializable(object.getClass());
        } catch (IOException e) {
            return false;
        }
    }

    private static ClassLoader setThreadContextClassLoader(ClassLoader loader) {
        return (loader != null) ? WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader) : null;
    }
}
