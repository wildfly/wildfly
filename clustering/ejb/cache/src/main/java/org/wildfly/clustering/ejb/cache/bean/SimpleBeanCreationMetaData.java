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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * A simple {@link BeanCreationMetaData} implementation
 * @author Paul Ferraro
 * @param <K> the bean group identifier type
 */
public class SimpleBeanCreationMetaData<K> implements BeanCreationMetaData<K> {

    private final String name;
    private final K groupId;
    private final Instant creationTime;

    public SimpleBeanCreationMetaData(String name, K groupId) {
        this(name, groupId, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    SimpleBeanCreationMetaData(String name, K groupId, Instant creationTime) {
        this.name = name;
        this.groupId = groupId;
        this.creationTime = creationTime;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public K getGroupId() {
        return this.groupId;
    }

    @Override
    public Instant getCreationTime() {
        return this.creationTime;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName()).append(" { ");
        builder.append("name = ").append(this.name);
        builder.append(", group = ").append(this.groupId);
        builder.append(", created = ").append(this.creationTime);
        return builder.append(" }").toString();
    }
}
