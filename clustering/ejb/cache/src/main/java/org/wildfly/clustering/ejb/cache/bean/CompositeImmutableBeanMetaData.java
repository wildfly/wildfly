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

import org.wildfly.clustering.ejb.bean.BeanExpiration;
import org.wildfly.clustering.ejb.bean.ImmutableBeanMetaData;

/**
 * An {@link ImmutableBeanMetaData} implementation composed from a {@link BeanCreationMetaData} and an {@link ImmutableBeanAccessMetaData}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public class CompositeImmutableBeanMetaData<K> implements ImmutableBeanMetaData<K> {

    private final BeanCreationMetaData<K> creationMetaData;
    private final ImmutableBeanAccessMetaData accessMetaData;
    private final BeanExpiration expiration;

    public CompositeImmutableBeanMetaData(BeanCreationMetaData<K> creationMetaData, ImmutableBeanAccessMetaData accessMetaData, BeanExpiration expiration) {
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
        this.expiration = expiration;
    }

    @Override
    public String getName() {
        return this.creationMetaData.getName();
    }

    @Override
    public K getGroupId() {
        return this.creationMetaData.getGroupId();
    }

    @Override
    public Instant getLastAccessTime() {
        return this.creationMetaData.getCreationTime().plus(this.accessMetaData.getLastAccessDuration());
    }

    @Override
    public Duration getTimeout() {
        return (this.expiration != null) ? this.expiration.getTimeout() : null;
    }
}
