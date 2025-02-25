/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.contexts;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class DistributedContextsSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public DistributedContextsSerializationContextInitializer() {
        super(PassivationCapableSerializableContextual.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new PassivationCapableSerializableBeanMarshaller<>());
        context.registerMarshaller(new PassivationCapableSerializableContextualMarshaller<>());
    }
}
