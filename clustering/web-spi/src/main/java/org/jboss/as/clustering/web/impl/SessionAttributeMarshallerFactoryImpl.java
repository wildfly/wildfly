/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.clustering.MarshallingContext;
import org.jboss.as.clustering.SimpleMarshalledValue;
import org.jboss.as.clustering.VersionedMarshallingConfiguration;
import org.jboss.as.clustering.web.DistributableSessionMetadata;
import org.jboss.as.clustering.web.LocalDistributableSessionManager;
import org.jboss.as.clustering.web.SessionAttributeMarshaller;
import org.jboss.as.clustering.web.SessionAttributeMarshallerFactory;
import org.jboss.marshalling.ClassTable;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.reflect.ReflectiveCreator;
import org.jboss.marshalling.reflect.SunReflectiveCreator;

/**
 * Default factory for creating session attribute marshallers.
 *
 * @author Paul Ferraro
 */
public class SessionAttributeMarshallerFactoryImpl implements SessionAttributeMarshallerFactory, ClassTable, VersionedMarshallingConfiguration {
    private static final int CURRENT_VERSION = 1;
    private final MarshallerFactory factory;
    private final Map<Integer, MarshallingConfiguration> configurations = new ConcurrentHashMap<Integer, MarshallingConfiguration>();

    public SessionAttributeMarshallerFactoryImpl() {
        this(Marshalling.getMarshallerFactory("river", Marshalling.class.getClassLoader()));
    }

    public SessionAttributeMarshallerFactoryImpl(MarshallerFactory factory) {
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.clustering.web.SessionAttributeMarshallerFactory#createMarshaller(org.jboss.as.clustering.web.LocalDistributableSessionManager)
     */
    @Override
    public SessionAttributeMarshaller createMarshaller(LocalDistributableSessionManager manager) {
        MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setClassResolver(manager.getApplicationClassResolver());
        configuration.setSerializedCreator(new SunReflectiveCreator());
        configuration.setExternalizerCreator(new ReflectiveCreator());
        configuration.setClassTable(this);
        this.configurations.put(CURRENT_VERSION, configuration);
        return new SessionAttributeMarshallerImpl(new MarshallingContext(this.factory, this));
    }

    @Override
    public int getCurrentMarshallingVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public MarshallingConfiguration getMarshallingConfiguration(int version) {
        MarshallingConfiguration config = this.configurations.get(version);
        if (config == null) {
            throw ClusteringWebMessages.MESSAGES.unsupportedMarshallingVersion(version);
        }
        return config;
    }

    private static final Class<?>[] classes = new Class<?>[] {
        DistributableSessionMetadata.class,
        SimpleMarshalledValue.class,
        Serializable.class,
    };

    private static final Map<Class<?>, Writer> writers = createWriters();
    private static Map<Class<?>, Writer> createWriters() {
        Map<Class<?>, Writer> writers = new IdentityHashMap<Class<?>, Writer>();
        for (int i = 0; i < classes.length; i++) {
            writers.put(classes[i], new ByteWriter((byte) i));
        }
        return writers;
    }

    @Override
    public Writer getClassWriter(Class<?> targetClass) throws IOException {
        return writers.get(targetClass);
    }

    @Override
    public Class<?> readClass(Unmarshaller unmarshaller) throws IOException, ClassNotFoundException {
        int index = unmarshaller.readUnsignedByte();
        if (index >= classes.length) {
            throw ClusteringWebMessages.MESSAGES.classIndexNotFoundInClassTable(this.getClass().getName(), index);
        }
        return classes[index];
    }

    private static final class ByteWriter implements Writer {
        final byte[] bytes;

        ByteWriter(final byte... bytes) {
            this.bytes = bytes;
        }

        @Override
        public void writeClass(final Marshaller marshaller, final Class<?> clazz) throws IOException {
            marshaller.write(bytes);
        }
    }
}
