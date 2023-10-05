/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;

import org.wildfly.clustering.ee.cache.function.RemappingFunction;
import org.wildfly.clustering.ee.cache.offset.Offset;

/**
 * @author Paul Ferraro
 */
public class SessionAccessMetaDataEntryFunction extends RemappingFunction<SessionAccessMetaDataEntry, SessionAccessMetaDataEntryOffsets> {

    public SessionAccessMetaDataEntryFunction(MutableSessionAccessMetaDataOffsetValues values) {
        this(new SessionAccessMetaDataEntryOffsets() {
            @Override
            public Offset<Duration> getSinceCreationOffset() {
                return values.getSinceCreation().getOffset();
            }

            @Override
            public Offset<Duration> getLastAccessOffset() {
                return values.getLastAccess().getOffset();
            }
        });
    }

    public SessionAccessMetaDataEntryFunction(SessionAccessMetaDataEntryOffsets offsets) {
        super(offsets);
    }
}
