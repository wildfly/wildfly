/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.util.function.BiFunction;

import org.wildfly.clustering.ejb.timer.TimerConfiguration;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;

/**
 * @author Paul Ferraro
 */
public interface TimerMetaDataCreationContext<MC> {

    Object getInfo();

    TimerConfiguration getConfiguration();

    BiFunction<MarshalledValue<Object, MC>, ? extends TimerConfiguration, TimerCreationMetaData<MC>> getCreationMetaDataFactory();
}
