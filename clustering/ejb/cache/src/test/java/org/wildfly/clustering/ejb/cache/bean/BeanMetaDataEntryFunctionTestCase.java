/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Instant;
import java.util.UUID;

import org.mockito.Mockito;
import org.wildfly.clustering.cache.Key;
import org.wildfly.clustering.server.offset.OffsetValue;

/**
 * @author Paul Ferraro
 */
public class BeanMetaDataEntryFunctionTestCase extends AbstractBeanMetaDataEntryTestCase {

    @Override
    public void accept(RemappableBeanMetaDataEntry<UUID> entry) {
        OffsetValue<Instant> operand = entry.getLastAccessTime().rebase();

        BeanMetaDataEntry<UUID> mutableEntry = new MutableBeanMetaDataEntry<>(entry, operand);

        this.updateState(mutableEntry);

        this.verifyOriginalState(entry);

        Key<UUID> key = Mockito.mock(Key.class);

        BeanMetaDataEntry<UUID> resultEntry = new BeanMetaDataEntryFunction<UUID>(operand).apply(key, entry);

        Mockito.verifyNoInteractions(key);

        this.verifyUpdatedState(resultEntry);
    }
}
