/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Instant;
import java.util.function.Supplier;

import org.wildfly.clustering.cache.function.RemappingFunction;
import org.wildfly.clustering.server.offset.Offset;
import org.wildfly.clustering.server.offset.OffsetValue;
import org.wildfly.common.function.Functions;

/**
 * Remapping function for a bean metadata entry.
 * @author Paul Ferraro
 */
public class BeanMetaDataEntryFunction<K> extends RemappingFunction<RemappableBeanMetaDataEntry<K>, Supplier<Offset<Instant>>> {

    public BeanMetaDataEntryFunction(OffsetValue<Instant> lastAccess) {
        super(lastAccess::getOffset);
    }

    BeanMetaDataEntryFunction(Offset<Instant> lastAccessOffset) {
        super(Functions.constantSupplier(lastAccessOffset));
    }

    public Offset<Instant> getOffset() {
        return super.getOperand().get();
    }
}
