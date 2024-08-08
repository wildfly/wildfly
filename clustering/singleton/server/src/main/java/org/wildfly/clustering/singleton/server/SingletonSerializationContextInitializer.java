/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * {@link org.infinispan.protostream.SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SingletonSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @SuppressWarnings("deprecation")
    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new SingletonElectionCommandMarshaller());
        context.registerMarshaller(ProtoStreamMarshaller.of(StartCommand.class));
        context.registerMarshaller(ProtoStreamMarshaller.of(StopCommand.class));
        context.registerMarshaller(ProtoStreamMarshaller.of(PrimaryProviderCommand.class));
        context.registerMarshaller(ProtoStreamMarshaller.of(SingletonValueCommand.class));
    }
}
