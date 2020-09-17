/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.spi.util.concurrent.atomic;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.BooleanExternalizer;
import org.wildfly.clustering.marshalling.spi.ExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.IntExternalizer;
import org.wildfly.clustering.marshalling.spi.LongExternalizer;
import org.wildfly.clustering.marshalling.spi.ObjectExternalizer;

/**
 * Externalizers for the java.util.concurrent.atomic package.
 * @author Paul Ferraro
 */
public enum AtomicExternalizerProvider implements ExternalizerProvider {

    ATOMIC_BOOLEAN(new BooleanExternalizer<>(AtomicBoolean.class, AtomicBoolean::new, AtomicBoolean::get)),
    ATOMIC_INTEGER(new IntExternalizer<>(AtomicInteger.class, AtomicInteger::new, AtomicInteger::get)),
    ATOMIC_LONG(new LongExternalizer<>(AtomicLong.class, AtomicLong::new, AtomicLong::get)),
    ATOMIC_REFERENCE(new ObjectExternalizer<>(AtomicReference.class, AtomicReference::new, AtomicReference::get)),
    ;
    private final Externalizer<?> externalizer;

    AtomicExternalizerProvider(Externalizer<?> externalizer) {
        this.externalizer = externalizer;
    }

    @Override
    public Externalizer<?> getExternalizer() {
        return this.externalizer;
    }
}
