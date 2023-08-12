/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Factory for creating a {@link SessionAttributes} object.
 * @param <C> the ServletContext specification type
 * @param <V> the marshalled value type
 * @author Paul Ferraro
 */
public interface SessionAttributesFactory<C, V> extends ImmutableSessionAttributesFactory<V>, Creator<String, V, Void>, Remover<String>, AutoCloseable {
    SessionAttributes createSessionAttributes(String id, V value, ImmutableSessionMetaData metaData, C context);

    @Override
    default void close() {
        // Nothing to close
    }
}
