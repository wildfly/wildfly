/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

/**
 * Unit test for {@link MutableSessionMetaDataEntry}.
 * @author Paul Ferraro
 */
public class MutableSessionMetaDataEntryTestCase extends AbstractSessionMetaDataEntryTestCase {

    @Override
    public void accept(ContextualSessionMetaDataEntry<Object> entry) {
        SessionMetaDataEntry mutableEntry = new MutableSessionMetaDataEntry(entry, MutableSessionMetaDataOffsetValues.from(entry));

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
