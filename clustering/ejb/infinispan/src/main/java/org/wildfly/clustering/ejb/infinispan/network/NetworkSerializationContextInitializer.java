/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.network;

import org.jboss.as.network.ClientMapping;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * Provides marshallers for the <code>org.jboss.as.network</code> package.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class NetworkSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public NetworkSerializationContextInitializer() {
        super(ClientMapping.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new ClientMappingMarshaller());
    }
}
