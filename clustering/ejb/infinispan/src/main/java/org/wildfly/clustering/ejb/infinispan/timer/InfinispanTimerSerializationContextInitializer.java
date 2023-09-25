/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.UUID;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.ejb.cache.timer.TimerIndexMarshaller;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.EnumMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;

/**
 * @author Paul Ferraro
 */
public class InfinispanTimerSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalMarshaller<>(InfinispanTimerMetaDataKey.class, UUID.class, InfinispanTimerMetaDataKey<UUID>::getId, InfinispanTimerMetaDataKey::new));
        context.registerMarshaller(new FunctionalMarshaller<>(InfinispanTimerIndexKey.class, TimerIndexMarshaller.INSTANCE, InfinispanTimerIndexKey::getId, InfinispanTimerIndexKey::new));
        context.registerMarshaller(new EnumMarshaller<>(TimerMetaDataKeyFilter.class));
    }
}
