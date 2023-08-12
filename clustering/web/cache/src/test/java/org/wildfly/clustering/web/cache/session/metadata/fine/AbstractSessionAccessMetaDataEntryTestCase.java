/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

/**
 * Abstract unit test for {@link SessionAccessMetaDataEntry} implementations.
 * @author Paul Ferraro
 */
public abstract class AbstractSessionAccessMetaDataEntryTestCase implements Consumer<SessionAccessMetaDataEntry> {

    private final Duration originalSinceCreation =  Duration.ofMinutes(1);
    private final Duration originalLastAccess = Duration.ofSeconds(1);

    private final Duration updatedSinceCreation = Duration.ofMinutes(2);
    private final Duration updatedLastAccess = Duration.ofSeconds(2);

    @Test
    public void test() {
        DefaultSessionAccessMetaDataEntry entry = new DefaultSessionAccessMetaDataEntry();

        // Verify defaults
        Assert.assertTrue(entry.getSinceCreationDuration().isZero());
        Assert.assertTrue(entry.getLastAccessDuration().isZero());

        // Apply original state
        entry.setLastAccessDuration(this.originalSinceCreation, this.originalLastAccess);

        this.verifyOriginalState(entry);

        this.accept(entry);
    }

    void updateState(SessionAccessMetaData entry) {
        entry.setLastAccessDuration(this.updatedSinceCreation, this.updatedLastAccess);
    }

    void verifyOriginalState(SessionAccessMetaData metaData) {
        Assert.assertEquals(this.originalSinceCreation, metaData.getSinceCreationDuration());
        Assert.assertEquals(this.originalLastAccess, metaData.getLastAccessDuration());
    }

    void verifyUpdatedState(SessionAccessMetaData metaData) {
        Assert.assertEquals(this.updatedSinceCreation, metaData.getSinceCreationDuration());
        Assert.assertEquals(this.updatedLastAccess, metaData.getLastAccessDuration());
    }
}
