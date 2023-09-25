/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.Map;

import org.wildfly.clustering.ee.Locator;
import org.wildfly.clustering.ejb.timer.ImmutableTimerMetaData;

/**
 * @author Paul Ferraro
 */
public interface ImmutableTimerMetaDataFactory<I, C> extends Locator<I, Map.Entry<TimerCreationMetaData<C>, TimerAccessMetaData>> {

    ImmutableTimerMetaData createImmutableTimerMetaData(Map.Entry<TimerCreationMetaData<C>, TimerAccessMetaData> entry);
}
