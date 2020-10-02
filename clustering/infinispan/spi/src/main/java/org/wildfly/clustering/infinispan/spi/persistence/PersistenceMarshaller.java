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

package org.wildfly.clustering.infinispan.spi.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.marshall.StreamAwareMarshaller;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry;
import org.infinispan.marshall.protostream.impl.SerializationContextRegistry.MarshallerType;
import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.infinispan.marshalling.AbstractMarshaller;

/**
 * @author Paul Ferraro
 */
@Scope(Scopes.GLOBAL)
public class PersistenceMarshaller extends AbstractMarshaller implements org.infinispan.marshall.persistence.PersistenceMarshaller {

    @Inject GlobalComponentRegistry registry;
    @Inject SerializationContextRegistry contextRegistry;

    private StreamAwareMarshaller streamAwareUserMarshaller;
    private Marshaller userMarshaller;

    @Start
    @Override
    public void start() {
        this.userMarshaller = this.registry.getGlobalConfiguration().serialization().marshaller();
        this.streamAwareUserMarshaller = (StreamAwareMarshaller) this.userMarshaller;
    }

    @Override
    public boolean isMarshallable(Object object){
        return this.streamAwareUserMarshaller.isMarshallable(object);
    }

    @Override
    public MediaType mediaType() {
        return this.userMarshaller.mediaType();
    }

    @Override
    public void writeObject(Object object, OutputStream output) throws IOException {
        this.streamAwareUserMarshaller.writeObject(object, output);
    }

    @Override
    public Object readObject(InputStream input) throws ClassNotFoundException, IOException {
        return this.streamAwareUserMarshaller.readObject(input);
    }

    @Override
    public void register(SerializationContextInitializer initializer) {
        this.contextRegistry.addContextInitializer(MarshallerType.PERSISTENCE, initializer);
    }

    @Override
    public Marshaller getUserMarshaller() {
        return this.userMarshaller;
    }
}
