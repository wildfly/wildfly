/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Abstract unit test for {@link SessionCreationMetaDataEntry} implementations.
 * @author Paul Ferraro
 */
public abstract class AbstractSessionCreationMetaDataEntryTestCase implements Consumer<SessionCreationMetaDataEntry<Object>> {

    private final Instant created = Instant.now();
    private final Duration originalTimeout = Duration.ofMinutes(20);

    private final Duration updatedTimeout = Duration.ofMinutes(30);

    @Test
    public void test() {
        DefaultSessionCreationMetaDataEntry<Object> entry = new DefaultSessionCreationMetaDataEntry<>(this.created);

        // Verify defaults
        Assert.assertEquals(this.created, entry.getCreationTime());
        Assert.assertEquals(Duration.ZERO, entry.getTimeout());
        Assert.assertNull(entry.getContext(() -> null));

        // Apply original state
        entry.setTimeout(this.originalTimeout);

        this.verifyOriginalState(entry);

        this.accept(entry);
    }

    void updateState(SessionCreationMetaData metaData) {
        metaData.setTimeout(this.updatedTimeout);
    }

    void verifyOriginalState(SessionCreationMetaData metaData) {
        Assert.assertEquals(this.created, metaData.getCreationTime());
        Assert.assertEquals(this.originalTimeout, metaData.getTimeout());
    }

    void verifyUpdatedState(SessionCreationMetaData metaData) {
        Assert.assertEquals(this.created, metaData.getCreationTime());
        Assert.assertEquals(this.updatedTimeout, metaData.getTimeout());
    }
}
