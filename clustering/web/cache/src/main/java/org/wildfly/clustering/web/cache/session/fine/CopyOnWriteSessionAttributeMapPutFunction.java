/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.fine;

import java.util.Map;
import java.util.UUID;

import org.wildfly.clustering.ee.cache.function.CopyOnWriteMapPutFunction;

/**
 * Copy-on-write {@link Map#put(Object, Object)} function for a session attribute.
 * @author Paul Ferraro
 */
public class CopyOnWriteSessionAttributeMapPutFunction extends CopyOnWriteMapPutFunction<String, UUID> {

    public CopyOnWriteSessionAttributeMapPutFunction(String attributeName, UUID attributeId) {
        super(new SessionAttributeMapEntry(attributeName, attributeId));
    }

    public CopyOnWriteSessionAttributeMapPutFunction(Map.Entry<String, UUID> operand) {
        super(operand);
    }
}
