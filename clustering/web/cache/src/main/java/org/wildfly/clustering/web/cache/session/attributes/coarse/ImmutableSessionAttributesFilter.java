/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.coarse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * @author Paul Ferraro
 */
public class ImmutableSessionAttributesFilter implements SessionAttributesFilter {

    private final ImmutableSessionAttributes attributes;

    public ImmutableSessionAttributesFilter(ImmutableSession session) {
        this(session.getAttributes());
    }

    public ImmutableSessionAttributesFilter(ImmutableSessionAttributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public <T> Map<String, T> getAttributes(Class<T> targetClass) {
        Set<String> names = this.attributes.getAttributeNames();
        if (names.isEmpty()) return Collections.emptyMap();
        Map<String, T> result = new HashMap<>(names.size());
        for (String name : names) {
            Object attribute = this.attributes.getAttribute(name);
            if (targetClass.isInstance(attribute)) {
                result.put(name, targetClass.cast(attribute));
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
