/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Instant;
import java.util.UUID;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ejb.cache.timer.TimerIndex;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class InfinispanTimerSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(context.getMarshaller(UUID.class).wrap(InfinispanTimerMetaDataKey.class, InfinispanTimerMetaDataKey<UUID>::getId, InfinispanTimerMetaDataKey::new));
        context.registerMarshaller(context.getMarshaller(TimerIndex.class).wrap(InfinispanTimerIndexKey.class, InfinispanTimerIndexKey::getId, InfinispanTimerIndexKey::new));
        context.registerMarshaller(ProtoStreamMarshaller.of(TimerCacheKeyFilter.class));
        context.registerMarshaller(ProtoStreamMarshaller.of(TimerCacheEntryFilter.class));
        context.registerMarshaller(Scalar.LONG.cast(Long.class).toMarshaller(Instant::toEpochMilli, Instant::ofEpochMilli).wrap(SimpleTimeoutMetaData.class, SimpleTimeoutMetaData::getNextTimeout, SimpleTimeoutMetaData::new));
    }
}
