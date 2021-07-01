/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import java.time.Duration;
import java.time.Instant;

/**
 * An immutable view of a cache entry for a bean.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 */
public interface ImmutableBeanEntry<G> {
    /**
     * Returns the group identifier associated with the bean entry
     * @return a group identifier
     */
    G getGroupId();

    /**
     * Returns the name of this bean
     * @return a bean name
     */
    String getBeanName();

    /**
     * Returns the last time this bean was accessed.
     * @return an instant in time
     */
    Instant getLastAccessedTime();

    /**
     * Indicates whether the bean is expired relative to the specified timeout
     * @param timeout a duration after which a bean should be considered to be expired.
     * @return true, if the bean is expired, false otherwise
     */
    default boolean isExpired(Duration timeout) {
        if ((timeout == null) || timeout.isNegative()) return false;
        if (timeout.isZero()) return true;
        Instant lastAccessedTime = this.getLastAccessedTime();
        return (lastAccessedTime != null) && !lastAccessedTime.plus(timeout).isAfter(Instant.now());
    }
}
