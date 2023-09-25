/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.elytron;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;

/**
 * Marshaller registration for the {@link org.wildfly.elytron.web.undertow.server.servlet} package.
 * @author Paul Ferraro
 */
public class ElytronUndertowSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public ElytronUndertowSerializationContextInitializer() {
        super("org.wildfly.elytron.web.undertow.server.servlet.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new IdentityContainerMarshaller());
    }
}
