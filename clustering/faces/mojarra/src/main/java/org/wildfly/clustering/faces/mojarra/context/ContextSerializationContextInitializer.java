/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.context;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class ContextSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public ContextSerializationContextInitializer() {
        super("com.sun.faces.context.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(ProtoStreamMarshaller.of(ContextImmutability::createMutex));
    }
}
