/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;
import java.util.UUID;

import org.junit.Assert;
import org.mockito.Mockito;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.cache.offset.OffsetValue;
import org.wildfly.common.function.Functions;

/**
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataEntryFunctionTestCase extends AbstractSessionCreationMetaDataEntryTestCase {

    @Override
    public void accept(SessionCreationMetaDataEntry<Object> entry) {
        Object context = UUID.randomUUID();
        Assert.assertSame(context, entry.getContext(Functions.constantSupplier(context)));
        Assert.assertSame(context, entry.getContext(Functions.constantSupplier(null)));

        OffsetValue<Duration> timeoutOffset = OffsetValue.from(entry.getTimeout());

        MutableSessionCreationMetaData mutableEntry = new MutableSessionCreationMetaData(entry, timeoutOffset);

        this.updateState(mutableEntry);

        this.verifyOriginalState(entry);

        Key<String> key = Mockito.mock(Key.class);

        SessionCreationMetaDataEntry<Object> resultEntry = new SessionCreationMetaDataEntryFunction<>(timeoutOffset).apply(key, entry);

        Mockito.verifyNoInteractions(key);

        this.verifyUpdatedState(resultEntry);

        Assert.assertSame(context, resultEntry.getContext(Functions.constantSupplier(null)));
    }
}
