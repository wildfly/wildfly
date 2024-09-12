/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.server.offset.Offset;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class TimerSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new IntervalTimerMetaDataEntryMarshaller());
        context.registerMarshaller(new ScheduleTimerMetaDataEntryMarshaller());
        @SuppressWarnings("unchecked")
        ProtoStreamMarshaller<Offset<Duration>> offsetMarshaller = context.getMarshaller((Class<Offset<Duration>>) Offset.forDuration(Duration.ZERO).getClass());
        context.registerMarshaller(offsetMarshaller.wrap(TimerMetaDataEntryFunction.class, TimerMetaDataEntryFunction::getOffset, TimerMetaDataEntryFunction::new));
        context.registerMarshaller(TimeoutDescriptorMarshaller.INSTANCE.asMarshaller());
        context.registerMarshaller(new TimerIndexMarshaller());
    }
}
