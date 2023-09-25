/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.util.UUID;

import org.wildfly.clustering.ejb.timer.IntervalTimerConfiguration;

/**
 * Unit test for {@link IntervalTimerMetaDataEntry}.
 * @author Paul Ferraro
 */
public class IntervalTimerMetaDataEntryTestCase extends AbstractIntervalTimerMetaDataEntryTestCase {

    public IntervalTimerMetaDataEntryTestCase(IntervalTimerConfiguration config) {
        super(config);
    }

    @Override
    public void accept(IntervalTimerMetaDataEntry<UUID> entry) {
        this.updateState(entry);
        this.verifyUpdatedState(entry);
    }
}
