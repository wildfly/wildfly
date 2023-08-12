/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import java.time.Duration;
import java.util.function.Supplier;

import org.wildfly.clustering.ee.cache.function.RemappingFunction;
import org.wildfly.clustering.ee.cache.offset.Offset;
import org.wildfly.clustering.ee.cache.offset.OffsetValue;
import org.wildfly.common.function.Functions;

/**
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataEntryFunction<C> extends RemappingFunction<SessionCreationMetaDataEntry<C>, Supplier<Offset<Duration>>> {

    public SessionCreationMetaDataEntryFunction(OffsetValue<Duration> operand) {
        super(operand::getOffset);
    }

    public SessionCreationMetaDataEntryFunction(Offset<Duration> operand) {
        super(Functions.constantSupplier(operand));
    }

    Offset<Duration> getOffset() {
        return this.getOperand().get();
    }
}
