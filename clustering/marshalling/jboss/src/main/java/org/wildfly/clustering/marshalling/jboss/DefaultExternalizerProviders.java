/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.jboss;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

import org.wildfly.clustering.marshalling.spi.ExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.MarshallingExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.net.NetExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.sql.SQLExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.time.TimeExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.util.UtilExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.util.concurrent.ConcurrentExternalizerProvider;
import org.wildfly.clustering.marshalling.spi.util.concurrent.atomic.AtomicExternalizerProvider;

/**
 * Default sets of externalizers.
 * @author Paul Ferraro
 */
public enum DefaultExternalizerProviders implements Supplier<Set<? extends ExternalizerProvider>> {
    NET(NetExternalizerProvider.class),
    SQL(SQLExternalizerProvider.class),
    TIME(TimeExternalizerProvider.class),
    UTIL(UtilExternalizerProvider.class),
    ATOMIC(AtomicExternalizerProvider.class),
    CONCURRENT(ConcurrentExternalizerProvider.class),
    MARSHALLING(MarshallingExternalizerProvider.class),
    ;
    private final Set<? extends ExternalizerProvider> providers;

    <E extends Enum<E> & ExternalizerProvider> DefaultExternalizerProviders(Class<E> providerClass) {
        this.providers = EnumSet.allOf(providerClass);
    }

    @Override
    public Set<? extends ExternalizerProvider> get() {
        return this.providers;
    }
}
