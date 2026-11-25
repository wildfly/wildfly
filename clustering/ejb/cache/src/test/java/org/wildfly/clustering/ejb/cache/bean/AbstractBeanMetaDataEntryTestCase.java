/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractBeanMetaDataEntryTestCase implements Consumer<RemappableBeanMetaDataEntry<UUID>> {

    private final Instant originalLastAccessed = Instant.now();
    private final Instant created = this.originalLastAccessed.minus(Duration.ofMinutes(1));
    private final String name = "foo";
    private final UUID groupId = UUID.randomUUID();

    private final Instant updatedLastAccessed = this.originalLastAccessed.plus(Duration.ofSeconds(10));

    @Test
    public void test() {
        RemappableBeanMetaDataEntry<UUID> entry = new DefaultBeanMetaDataEntry<>(this.name, this.groupId, this.created);

        // Verify defaults
        Assert.assertEquals(this.name, entry.getName());
        Assert.assertEquals(this.groupId, entry.getGroupId());
        Assert.assertEquals(this.created, entry.getLastAccessTime().getBasis());
        Assert.assertEquals(this.created, entry.getLastAccessTime().get());

        // Apply original state
        entry.getLastAccessTime().set(this.originalLastAccessed);

        this.verifyOriginalState(entry);

        this.accept(entry);
    }

    void updateState(BeanMetaDataEntry<UUID> entry) {
        entry.getLastAccessTime().set(this.updatedLastAccessed);
    }

    void verifyOriginalState(BeanMetaDataEntry<UUID> entry) {
        Assert.assertEquals(this.name, entry.getName());
        Assert.assertEquals(this.groupId, entry.getGroupId());
        Assert.assertEquals(this.originalLastAccessed, entry.getLastAccessTime().get());
    }

    void verifyUpdatedState(BeanMetaDataEntry<UUID> entry) {
        Assert.assertEquals(this.name, entry.getName());
        Assert.assertEquals(this.groupId, entry.getGroupId());
        Assert.assertEquals(this.updatedLastAccessed, entry.getLastAccessTime().get());
    }
}
