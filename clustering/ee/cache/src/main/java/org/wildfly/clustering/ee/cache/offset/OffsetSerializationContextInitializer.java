/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.cache.offset;

import java.time.Duration;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class OffsetSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalMarshaller<>(Offset.DurationOffset.class, Duration.class, Offset.DurationOffset::get, Offset.DurationOffset::new));
        context.registerMarshaller(new FunctionalMarshaller<>(Offset.InstantOffset.class, Duration.class, Offset.InstantOffset::get, Offset.InstantOffset::new));
    }
}
