/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
