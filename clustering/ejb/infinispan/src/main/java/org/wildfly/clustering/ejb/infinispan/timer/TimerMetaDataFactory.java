/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.Map;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ejb.timer.TimerMetaData;

/**
 * @author Paul Ferraro
 */
public interface TimerMetaDataFactory<I, V> extends ImmutableTimerMetaDataFactory<I, V>, Creator<I, Map.Entry<TimerCreationMetaData<V>, TimerAccessMetaData>, Map.Entry<TimerCreationMetaData<V>, TimerIndex>>, Remover<I> {

    TimerMetaData createTimerMetaData(I id, Map.Entry<TimerCreationMetaData<V>, TimerAccessMetaData> entry);
}
