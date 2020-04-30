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
