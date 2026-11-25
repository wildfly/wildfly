/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.elytron;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.elytron.web.undertow.server.servlet.ServletSecurityContextImpl.IdentityContainer;

/**
 * Marshaller registration for the {@link org.wildfly.elytron.web.undertow.server.servlet} package.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class ElytronUndertowSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public ElytronUndertowSerializationContextInitializer() {
        super(IdentityContainer.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new IdentityContainerMarshaller());
    }
}
