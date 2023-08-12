/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session.metadata.fine;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Test;
import org.wildfly.clustering.ee.Mutator;
import org.wildfly.clustering.web.cache.session.metadata.InvalidatableSessionMetaData;

/**
 * Unit test for {@link CompositeSessionMetaData}.
 * @author Paul Ferraro
 */
public class CompositeSessionMetaDataTestCase extends CompositeImmutableSessionMetaDataTestCase {
    private final SessionCreationMetaData creationMetaData;
    private final SessionAccessMetaData accessMetaData;
    private final Mutator mutator;

    private final InvalidatableSessionMetaData metaData;

    public CompositeSessionMetaDataTestCase() {
        this(mock(SessionCreationMetaData.class), mock(SessionAccessMetaData.class), mock(Mutator.class));
    }

    private CompositeSessionMetaDataTestCase(SessionCreationMetaData creationMetaData, SessionAccessMetaData accessMetaData, Mutator mutator) {
        this(creationMetaData, accessMetaData, mutator, new CompositeSessionMetaData(creationMetaData, accessMetaData, mutator));
    }

    private CompositeSessionMetaDataTestCase(SessionCreationMetaData creationMetaData, SessionAccessMetaData accessMetaData, Mutator mutator, InvalidatableSessionMetaData metaData) {
        super(creationMetaData, accessMetaData, metaData);
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
        this.mutator = mutator;
        this.metaData = metaData;
    }

    @Test
    public void setLastAccessedTime() {
        // New session
        Instant endTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Duration lastAccess = Duration.ofSeconds(1L);
        Instant startTime = endTime.minus(lastAccess);

        when(this.creationMetaData.getCreationTime()).thenReturn(startTime);

        this.metaData.setLastAccess(startTime, endTime);

        verify(this.accessMetaData).setLastAccessDuration(Duration.ZERO, lastAccess);
        verifyNoInteractions(this.mutator);

        reset(this.creationMetaData, this.accessMetaData);

        // Existing session
        Duration sinceCreated = Duration.ofSeconds(10L);

        when(this.creationMetaData.getCreationTime()).thenReturn(startTime.minus(sinceCreated));

        this.metaData.setLastAccess(startTime, endTime);

        verify(this.accessMetaData).setLastAccessDuration(sinceCreated, lastAccess);
        verifyNoInteractions(this.mutator);
    }

    @Test
    public void setMaxInactiveInterval() {
        Duration duration = Duration.ZERO;

        this.metaData.setTimeout(duration);

        verify(this.creationMetaData).setTimeout(duration);
        verifyNoInteractions(this.mutator);
    }

    @Test
    public void close() {
        this.metaData.close();

        verify(this.mutator).mutate();
    }
}
