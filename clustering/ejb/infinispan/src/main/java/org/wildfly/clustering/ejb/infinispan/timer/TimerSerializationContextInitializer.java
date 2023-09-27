/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.UUID;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.EnumMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;

/**
 * @author Paul Ferraro
 */
public class TimerSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalMarshaller<>(TimerCreationMetaDataKey.class, UUID.class, TimerCreationMetaDataKey<UUID>::getId, TimerCreationMetaDataKey::new));
        context.registerMarshaller(new FunctionalMarshaller<>(TimerAccessMetaDataKey.class, UUID.class, TimerAccessMetaDataKey<UUID>::getId, TimerAccessMetaDataKey::new));
        context.registerMarshaller(new IntervalTimerCreationMetaDataMarshaller());
        context.registerMarshaller(new ScheduleTimerCreationMetaDataMarshaller());
        context.registerMarshaller(new FunctionalMarshaller<>(TimerIndexKey.class, TimerIndexMarshaller.INSTANCE, TimerIndexKey::getId, TimerIndexKey::new));
        context.registerMarshaller(new EnumMarshaller<>(TimerCreationMetaDataKeyFilter.class));
    }
}
