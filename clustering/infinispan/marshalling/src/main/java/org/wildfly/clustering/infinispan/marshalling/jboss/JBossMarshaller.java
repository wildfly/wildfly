/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.marshalling.jboss;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap;

import org.infinispan.commons.dataconversion.MediaType;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.marshalling.SimpleDataOutput;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.marshalling.AbstractMarshaller;
import org.wildfly.clustering.marshalling.jboss.MarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.MarshallingContext;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingConfigurationRepository;
import org.wildfly.clustering.marshalling.jboss.SimpleMarshallingContextFactory;
import org.wildfly.clustering.marshalling.spi.IndexSerializer;

/**
 * @author Paul Ferraro
 */
public class JBossMarshaller extends AbstractMarshaller {

    private final MarshallingContext context;

    public JBossMarshaller(Module module) {
        this(module.getModuleLoader(), module);
    }

    public JBossMarshaller(ModuleLoader loader, Module module) {
        this(new SimpleMarshallingConfigurationRepository(JBossMarshallingVersion.class, JBossMarshallingVersion.CURRENT, new AbstractMap.SimpleImmutableEntry<>(loader, module)), module.getClassLoader());
    }

    public JBossMarshaller(MarshallingConfigurationRepository repository, ClassLoader loader) {
        this(new SimpleMarshallingContextFactory().createMarshallingContext(repository, loader));
    }

    public JBossMarshaller(MarshallingContext context) {
        this.context = context;
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_JBOSS_MARSHALLING;
    }

    @Override
    public boolean isMarshallable(Object object) {
        return this.context.isMarshallable(object);
    }

    @Override
    public void writeObject(Object object, OutputStream output) throws IOException {
        int version = this.context.getCurrentVersion();
        try (SimpleDataOutput data = new SimpleDataOutput(Marshalling.createByteOutput(output))) {
            IndexSerializer.UNSIGNED_BYTE.writeInt(data, version);
            try (Marshaller marshaller = this.context.createMarshaller(version)) {
                marshaller.start(data);
                marshaller.writeObject(object);
                marshaller.finish();
            }
        }
    }

    @Override
    public Object readObject(InputStream input) throws ClassNotFoundException, IOException {
        try (SimpleDataInput data = new SimpleDataInput(Marshalling.createByteInput(input))) {
            int version = IndexSerializer.UNSIGNED_BYTE.readInt(data);
            try (Unmarshaller unmarshaller = this.context.createUnmarshaller(version)) {
                unmarshaller.start(data);
                Object result = unmarshaller.readObject();
                unmarshaller.finish();
                return result;
            }
        }
    }
}
