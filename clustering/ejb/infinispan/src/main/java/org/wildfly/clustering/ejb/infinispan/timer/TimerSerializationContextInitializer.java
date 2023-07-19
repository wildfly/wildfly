/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
