/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.coarse;

import java.util.Map;

/**
 * @author Paul Ferraro
 */
public interface SessionAttributesFilter {
    /**
     * Returns the attributes who values implement the specified type.
     * @param <T> the target instanceof type
     * @param targetClass a target class/interface.
     * @return a map of session attribute names and values that are instances of the specified type.
     */
    <T> Map<String, T> getAttributes(Class<T> targetClass);
}
