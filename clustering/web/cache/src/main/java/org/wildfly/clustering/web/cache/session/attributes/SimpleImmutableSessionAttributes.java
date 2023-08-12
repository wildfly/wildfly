/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session.attributes;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.wildfly.clustering.web.session.ImmutableSessionAttributes;

/**
 * An immutable "snapshot" of a session's attributes which can be accessed outside the scope of a transaction.
 * @author Paul Ferraro
 */
public class SimpleImmutableSessionAttributes implements ImmutableSessionAttributes {

    private final Map<String, Object> attributes;

    public SimpleImmutableSessionAttributes(ImmutableSessionAttributes attributes) {
        this(attributes.getAttributeNames().stream().collect(Collectors.toMap(Function.identity(), attributes::getAttribute)));
    }

    public SimpleImmutableSessionAttributes(Map<String, Object> attributes) {
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    @Override
    public Set<String> getAttributeNames() {
        return this.attributes.keySet();
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }
}
