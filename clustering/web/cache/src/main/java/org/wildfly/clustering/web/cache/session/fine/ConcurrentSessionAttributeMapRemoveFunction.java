/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.fine;

import java.util.UUID;

import org.wildfly.clustering.ee.cache.function.ConcurrentMapRemoveFunction;

/**
 * Concurrent {@link java.util.Map#remove(Object)} function for a session attribute.
 * @author Paul Ferraro
 */
public class ConcurrentSessionAttributeMapRemoveFunction extends ConcurrentMapRemoveFunction<String, UUID> {

    public ConcurrentSessionAttributeMapRemoveFunction(String attributeName) {
        super(attributeName);
    }
}
