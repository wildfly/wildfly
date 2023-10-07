/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.time.Duration;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.ee.cache.offset.Offset;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FieldSetProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;

/**
 * @author Paul Ferraro
 */
public class TimerSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @SuppressWarnings("unchecked")
    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new IntervalTimerMetaDataEntryMarshaller());
        context.registerMarshaller(new ScheduleTimerMetaDataEntryMarshaller());
        context.registerMarshaller(new FunctionalMarshaller<>(TimerMetaDataEntryFunction.class, Offset.forDuration(Duration.ZERO).getClass().asSubclass(Offset.class), TimerMetaDataEntryFunction::getOffset, TimerMetaDataEntryFunction::new));
        context.registerMarshaller(new FieldSetProtoStreamMarshaller<>(TimeoutDescriptorMarshaller.INSTANCE));
    }
}
