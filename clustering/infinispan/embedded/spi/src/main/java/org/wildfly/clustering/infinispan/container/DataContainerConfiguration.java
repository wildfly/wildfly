/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.container;

import java.util.function.Predicate;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.attributes.IdentityAttributeCopier;
import org.infinispan.commons.configuration.attributes.Matchable;

/**
 * @author Paul Ferraro
 */
@BuiltBy(DataContainerConfigurationBuilder.class)
public class DataContainerConfiguration implements Matchable<DataContainerConfiguration> {
    private static final Predicate<Object> ALWAYS = new Predicate<>() {
        @Override
        public boolean test(Object key) {
            return true;
        }
    };

    @SuppressWarnings("rawtypes")
    static final AttributeDefinition<Predicate> EVICTABLE_PREDICATE = AttributeDefinition.builder("evictable", ALWAYS, Predicate.class)
            .copier(IdentityAttributeCopier.identityCopier())
            .immutable()
            .build();

    private final AttributeSet attributes;
    @SuppressWarnings("rawtypes")
    private final Attribute<Predicate> evictable;

    DataContainerConfiguration(AttributeSet attributes) {
        this.attributes = attributes;
        this.evictable = attributes.attribute(EVICTABLE_PREDICATE);
    }

    public AttributeSet attributes() {
        return this.attributes;
    }

    public <K> Predicate<K> evictable() {
        return this.evictable.get();
    }

    @Override
    public boolean matches(DataContainerConfiguration configuration) {
        return this.evictable() == configuration.evictable();
    }
}
