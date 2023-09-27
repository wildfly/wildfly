/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.server.infinispan;

import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.CompositeSerializationContextInitializer;

/**
 * {@SerializationContextInitializer} service for this module.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class ServerSerializationContextInitializer extends CompositeSerializationContextInitializer {

    public ServerSerializationContextInitializer() {
        super(ServerSerializationContextInitializerProvider.class);
    }
}
