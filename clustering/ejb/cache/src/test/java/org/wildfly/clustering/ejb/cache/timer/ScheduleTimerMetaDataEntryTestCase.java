/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.timer;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import org.wildfly.clustering.ejb.timer.ScheduleTimerConfiguration;

/**
 * Unit test for {@link ScheduleTimerMetaDataEntry}.
 * @author Paul Ferraro
 */
public class ScheduleTimerMetaDataEntryTestCase extends AbstractScheduleTimerMetaDataEntryTestCase {

    public ScheduleTimerMetaDataEntryTestCase(Map.Entry<ScheduleTimerConfiguration, Method> entry) {
        super(entry);
    }

    @Override
    public void accept(ScheduleTimerMetaDataEntry<UUID> entry) {
        this.updateState(entry);
        this.verifyUpdatedState(entry);
    }
}
