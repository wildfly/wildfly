/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.fine;

import java.util.Map;
import java.util.UUID;

import org.wildfly.clustering.ee.cache.function.ConcurrentMapPutFunction;

/**
 * Concurrent {@link Map#put(Object, Object)} function for a session attribute.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link org.wildfly.clustering.ee.cache.function.MapComputeFunction}.
 */
@Deprecated(forRemoval = true)
public class ConcurrentSessionAttributeMapPutFunction extends ConcurrentMapPutFunction<String, UUID> {

    public ConcurrentSessionAttributeMapPutFunction(String attributeName, UUID attributeId) {
        super(new SessionAttributeMapEntry(attributeName, attributeId));
    }

    public ConcurrentSessionAttributeMapPutFunction(Map.Entry<String, UUID> operand) {
        super(operand);
    }
}
