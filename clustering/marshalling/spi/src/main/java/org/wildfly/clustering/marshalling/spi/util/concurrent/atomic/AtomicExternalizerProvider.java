/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
