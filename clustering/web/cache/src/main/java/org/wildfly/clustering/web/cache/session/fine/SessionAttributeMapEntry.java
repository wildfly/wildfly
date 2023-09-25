/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.fine;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.UUID;

/**
 * {@link java.util.Map.Entry} for a session attribute.
 * Used to optimize the marshalling of {@link java.util.Map#put(Object, Object)} functions.
 * @author Paul Ferraro
 */
public class SessionAttributeMapEntry extends SimpleImmutableEntry<String, UUID> {
    private static final long serialVersionUID = -6446474741366055972L;

    public SessionAttributeMapEntry(String key, UUID value) {
        super(key, value);
    }
}
