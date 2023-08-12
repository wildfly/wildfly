/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;

import org.wildfly.clustering.ee.cache.offset.OffsetValue;

/**
 * @author Paul Ferraro
 */
public interface MutableSessionAccessMetaDataOffsetValues extends MutableSessionAccessMetaDataValues {

    static MutableSessionAccessMetaDataOffsetValues from(ImmutableSessionAccessMetaData accessMetaData) {
        OffsetValue<Duration> sinceCreation = OffsetValue.from(accessMetaData.getSinceCreationDuration());
        OffsetValue<Duration> lastAccess = OffsetValue.from(accessMetaData.getLastAccessDuration());
        return new MutableSessionAccessMetaDataOffsetValues() {
            @Override
            public OffsetValue<Duration> getSinceCreation() {
                return sinceCreation;
            }

            @Override
            public OffsetValue<Duration> getLastAccess() {
                return lastAccess;
            }
        };
    }

    @Override
    OffsetValue<Duration> getSinceCreation();

    @Override
    OffsetValue<Duration> getLastAccess();
}
