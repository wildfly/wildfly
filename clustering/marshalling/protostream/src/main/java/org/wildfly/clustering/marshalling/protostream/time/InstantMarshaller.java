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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * @author Paul Ferraro
 */
public class InstantMarshaller extends FunctionalMarshaller<Instant, Duration> {

    private static final ExceptionFunction<Instant, Duration, IOException> DURATION_SINCE_EPOCH = new ExceptionFunction<Instant, Duration, IOException>() {
        @Override
        public Duration apply(Instant instant) {
            return Duration.ofSeconds(instant.getEpochSecond(), instant.getNano());
        }
    };
    private static final ExceptionFunction<Duration, Instant, IOException> FACTORY = new ExceptionFunction<Duration, Instant, IOException>() {
        @Override
        public Instant apply(Duration duration) {
            return Instant.ofEpochSecond(duration.getSeconds(), duration.getNano());
        }
    };

    public InstantMarshaller() {
        super(Instant.class, Duration.class, DURATION_SINCE_EPOCH, FACTORY);
    }
}
