/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

/**
 * @author Paul Ferraro
 */
public class MutableSessionAccessMetaDataTestCase extends AbstractSessionAccessMetaDataEntryTestCase {

    @Override
    public void accept(SessionAccessMetaDataEntry entry) {
        SessionAccessMetaData mutableEntry = new MutableSessionAccessMetaData(entry, MutableSessionAccessMetaDataOffsetValues.from(entry));

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
