/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.time;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * @author Paul Ferraro
 */
public class InstantMarshaller extends FunctionalMarshaller<Instant, Duration> {

    private static final ExceptionFunction<Instant, Duration, IOException> DURATION_SINCE_EPOCH = new ExceptionFunction<>() {
        @Override
        public Duration apply(Instant instant) {
            return Duration.ofSeconds(instant.getEpochSecond(), instant.getNano());
        }
    };
    private static final ExceptionFunction<Duration, Instant, IOException> FACTORY = new ExceptionFunction<>() {
        @Override
        public Instant apply(Duration duration) {
            return Instant.ofEpochSecond(duration.getSeconds(), duration.getNano());
        }
    };

    public InstantMarshaller() {
        super(Instant.class, Duration.class, DURATION_SINCE_EPOCH, FACTORY);
    }
}
