/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import org.mockito.Mockito;
import org.wildfly.clustering.ee.Key;

/**
 * @author Paul Ferraro
 */
public class SessionAccessMetaDataEntryFunctionTestCase extends AbstractSessionAccessMetaDataEntryTestCase {

    @Override
    public void accept(SessionAccessMetaDataEntry entry) {
        MutableSessionAccessMetaDataOffsetValues offsetValues = MutableSessionAccessMetaDataOffsetValues.from(entry);

        MutableSessionAccessMetaData mutableEntry = new MutableSessionAccessMetaData(entry, offsetValues);

        this.updateState(mutableEntry);

        this.verifyOriginalState(entry);

        Key<String> key = Mockito.mock(Key.class);

        SessionAccessMetaDataEntry resultEntry = new SessionAccessMetaDataEntryFunction(offsetValues).apply(key, entry);

        Mockito.verifyNoInteractions(key);

        this.verifyUpdatedState(resultEntry);
    }
}
