/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.Mutator;
import org.wildfly.clustering.web.annotation.Immutable;

/**
 * Ensures that an object that exists in the distributed cache replicates.
 * @author Paul Ferraro
 */
public class CacheMutator<K, V> implements Mutator {

    private static List<Object> IMMUTABLE_VALUES = Arrays.asList(
            null,
            Collections.EMPTY_LIST,
            Collections.EMPTY_MAP,
            Collections.EMPTY_SET
    );

    private static List<Class<?>> IMMUTABLE_TYPES = Arrays.<Class<?>>asList(
            BigDecimal.class,
            BigInteger.class,
            Boolean.class,
            Byte.class,
            Character.class,
            Currency.class,
            Double.class,
            Enum.class, // Strictly speaking, one could implement a mutable enum, but that would just be weird.
            File.class,
            Float.class,
            InetAddress.class,
            InetSocketAddress.class,
            Integer.class,
            Locale.class,
            Long.class,
            MathContext.class,
            Path.class,
            Permission.class,
            Short.class,
            StackTraceElement.class,
            String.class,
            TimeZone.class, // Strictly speaking, this class is mutable, although in practice it is never mutated.
            URI.class,
            URL.class,
            UUID.class
    );

    private final Cache<K, V> cache;
    private final CacheInvoker invoker;
    private final K id;
    private final V value;
    private final Set<Flag> flags;
    private final AtomicBoolean mutated = new AtomicBoolean(false);

    public CacheMutator(Cache<K, V> cache, CacheInvoker invoker, K id, V value, Flag... flags) {
        this.cache = cache;
        this.invoker = invoker;
        this.id = id;
        this.value = value;
        this.flags = EnumSet.of(Flag.IGNORE_RETURN_VALUES, flags);
    }

    @Override
    public void mutate() {
        // We only ever have to perform a replace once within a batch
        if (this.mutated.compareAndSet(false, true)) {
            this.invoker.invoke(this.cache, new MutateOperation<>(this.id, this.value), this.flags.toArray(new Flag[this.flags.size()]));
        }
    }

    public static boolean isMutable(Object object) {
        for (Object value: IMMUTABLE_VALUES) {
            if (object == value) return false;
        }
        for (Class<?> immutableClass: IMMUTABLE_TYPES) {
            if (immutableClass.isInstance(object)) return false;
        }
        return !object.getClass().isAnnotationPresent(Immutable.class);
    }
}