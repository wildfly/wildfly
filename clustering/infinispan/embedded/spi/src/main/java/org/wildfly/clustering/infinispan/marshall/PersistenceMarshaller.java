/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshall;

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
