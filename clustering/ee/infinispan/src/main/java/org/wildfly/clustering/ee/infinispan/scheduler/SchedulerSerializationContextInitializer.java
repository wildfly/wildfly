/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.scheduler;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.ValueMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SchedulerSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalScalarMarshaller<>(CancelCommand.class, Scalar.ANY, CancelCommand::getId, CancelCommand::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(ContainsCommand.class, Scalar.ANY, ContainsCommand::getId, ContainsCommand::new));
        context.registerMarshaller(new FunctionalScalarMarshaller<>(ScheduleWithTransientMetaDataCommand.class, Scalar.ANY, ScheduleWithTransientMetaDataCommand::getId, ScheduleWithTransientMetaDataCommand::new));
        context.registerMarshaller(new ScheduleWithMetaDataCommandMarshaller<>());
        context.registerMarshaller(new ValueMarshaller<>(new EntriesCommand<>()));
    }
}
