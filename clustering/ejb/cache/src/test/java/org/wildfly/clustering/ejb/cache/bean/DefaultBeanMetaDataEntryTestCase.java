/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.util.UUID;

/**
 * @author Paul Ferraro
 */
public class DefaultBeanMetaDataEntryTestCase extends AbstractBeanMetaDataEntryTestCase {

    @Override
    public void accept(RemappableBeanMetaDataEntry<UUID> entry) {
        this.updateState(entry);
        this.verifyUpdatedState(entry);
    }
}
