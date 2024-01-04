/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes;

import org.wildfly.clustering.ee.Locator;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * Factory for creating {@link ImmutableSessionAttributes} objects.
 * @author Paul Ferraro
 * @param <V> attributes cache entry type
 */
public interface ImmutableSessionAttributesFactory<V> extends Locator<String, V> {
    ImmutableSessionAttributes createImmutableSessionAttributes(String id, V value);
}
