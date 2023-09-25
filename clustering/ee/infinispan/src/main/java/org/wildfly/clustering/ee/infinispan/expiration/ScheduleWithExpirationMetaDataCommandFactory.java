/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ee.infinispan.expiration;

import java.util.function.BiFunction;

import org.wildfly.clustering.ee.expiration.ExpirationMetaData;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleCommand;
import org.wildfly.clustering.ee.infinispan.scheduler.ScheduleWithMetaDataCommand;

/**
 * {@link ScheduleCommand} factory that wraps expiration metadata with a marshallable implementation.
 * @author Paul Ferraro
 * @param <I> the identifier type of the scheduled object
 */
public class ScheduleWithExpirationMetaDataCommandFactory<I> implements BiFunction<I, ExpirationMetaData, ScheduleCommand<I, ExpirationMetaData>> {

    @Override
    public ScheduleCommand<I, ExpirationMetaData> apply(I id, ExpirationMetaData metaData) {
        return new ScheduleWithMetaDataCommand<>(id, new SimpleExpirationMetaData(metaData));
    }
}
