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

package org.wildfly.clustering.marshalling.protostream.time;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum InstantMarshaller implements ProtoStreamMarshallerProvider {
    INSTANCE;

    private final Function<Instant, Duration> durationSinceEpoch = new Function<Instant, Duration>() {
        @Override
        public Duration apply(Instant instant) {
            return Duration.ofSeconds(instant.getEpochSecond(), instant.getNano());
        }
    };
    private final Function<Duration, Instant> factory = new Function<Duration, Instant>() {
        @Override
        public Instant apply(Duration duration) {
            return Instant.ofEpochSecond(duration.getSeconds(), duration.getNano());
        }
    };
    private final ProtoStreamMarshaller<Instant> marshaller = new FunctionalMarshaller<>(Instant.class, DurationMarshaller.INSTANCE.cast(Duration.class), this.durationSinceEpoch, this.factory);

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
