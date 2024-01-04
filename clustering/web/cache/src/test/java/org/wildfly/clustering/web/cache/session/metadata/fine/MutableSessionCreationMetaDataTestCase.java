/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import org.wildfly.clustering.ee.cache.offset.OffsetValue;

/**
 * @author Paul Ferraro
 */
public class MutableSessionCreationMetaDataTestCase extends AbstractSessionCreationMetaDataEntryTestCase {

    @Override
    public void accept(SessionCreationMetaDataEntry<Object> entry) {
        SessionCreationMetaData mutableEntry = new MutableSessionCreationMetaData(entry, OffsetValue.from(entry.getTimeout()));

        // Verify decorator reflects current values
        this.verifyOriginalState(mutableEntry);

        // Mutate decorator
        this.updateState(mutableEntry);

        // Verify mutated state
        this.verifyUpdatedState(mutableEntry);

        // Verify original state of decorated object
        this.verifyOriginalState(entry);
    }
}
