/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import java.util.UUID;

import org.junit.Assert;
import org.mockito.Mockito;
import org.wildfly.clustering.ee.Key;
import org.wildfly.common.function.Functions;

/**
 * Unit test for {@link SessionMetaDataEntryFunction}.
 * @author Paul Ferraro
 */
public class SessionMetaDataEntryFunctionTestCase extends AbstractSessionMetaDataEntryTestCase {

    @Override
    public void accept(ContextualSessionMetaDataEntry<Object> entry) {
        Object context = UUID.randomUUID();
        Assert.assertSame(context, entry.getContext(Functions.constantSupplier(context)));
        Assert.assertSame(context, entry.getContext(Functions.constantSupplier(null)));

        MutableSessionMetaDataOffsetValues delta = MutableSessionMetaDataOffsetValues.from(entry);

        MutableSessionMetaDataEntry mutableEntry = new MutableSessionMetaDataEntry(entry, delta);

        this.updateState(mutableEntry);

        this.verifyOriginalState(entry);

        Key<String> key = Mockito.mock(Key.class);

        ContextualSessionMetaDataEntry<Object> resultEntry = new SessionMetaDataEntryFunction<>(delta).apply(key, entry);

        Mockito.verifyNoInteractions(key);

        this.verifyUpdatedState(resultEntry);

        Assert.assertSame(context, resultEntry.getContext(Functions.constantSupplier(null)));
    }
}
