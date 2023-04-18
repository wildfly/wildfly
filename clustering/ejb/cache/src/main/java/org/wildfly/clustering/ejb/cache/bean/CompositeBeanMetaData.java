/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.wildfly.clustering.ejb.bean.BeanExpiration;
import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * A {@link BeanMetaData} implementation composed from a {@link BeanCreationMetaData} and a {@link BeanAccessMetaData}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public class CompositeBeanMetaData<K> extends CompositeImmutableBeanMetaData<K> implements BeanMetaData<K> {

    private final BeanCreationMetaData<K> creationMetaData;
    private final BeanAccessMetaData accessMetaData;

    public CompositeBeanMetaData(BeanCreationMetaData<K> creationMetaData, BeanAccessMetaData accessMetaData, BeanExpiration expiration) {
        super(creationMetaData, accessMetaData, expiration);
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
    }

    @Override
    public void setLastAccessTime(Instant lastAccessedTime) {
        // Only retain millisecond precision
        Duration duration = Duration.between(this.creationMetaData.getCreationTime(), lastAccessedTime);
        this.accessMetaData.setLastAccessDuration((duration.getNano() == 0) ? duration : duration.truncatedTo(ChronoUnit.MILLIS).plusMillis(1));
    }
}
