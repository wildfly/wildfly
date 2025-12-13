/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.util.UUID;

/**
 * @author Paul Ferraro
 */
public class MutableBeanMetaDataEntryTestCase extends AbstractBeanMetaDataEntryTestCase {

    @Override
    public void accept(RemappableBeanMetaDataEntry<UUID> entry) {
        BeanMetaDataEntry<UUID> mutableEntry = new MutableBeanMetaDataEntry<>(entry, entry.getLastAccessTime().rebase());

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
