/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.fine;

import java.util.UUID;

import org.wildfly.clustering.ee.cache.function.CopyOnWriteMapRemoveFunction;

/**
 * Copy-on-write {@link java.util.Map#remove(Object)} function for a session attribute.
 * @author Paul Ferraro
 * @deprecated Superseded by {@link org.wildfly.clustering.ee.cache.function.MapComputeFunction}.
 */
@Deprecated(forRemoval = true)
public class CopyOnWriteSessionAttributeMapRemoveFunction extends CopyOnWriteMapRemoveFunction<String, UUID> {

    public CopyOnWriteSessionAttributeMapRemoveFunction(String attributeName) {
        super(attributeName);
    }
}
