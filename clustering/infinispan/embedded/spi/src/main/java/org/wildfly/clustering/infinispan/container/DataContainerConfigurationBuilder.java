/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.container;

import static org.wildfly.clustering.infinispan.container.DataContainerConfiguration.*;

import java.util.function.Predicate;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.Combine;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.ConfigurationBuilder;

/**
 * @author Paul Ferraro
 */
public class DataContainerConfigurationBuilder implements Builder<DataContainerConfiguration> {

    private final AttributeSet attributes;

    public DataContainerConfigurationBuilder(ConfigurationBuilder builder) {
        this.attributes = new AttributeSet(DataContainerConfiguration.class, EVICTABLE_PREDICATE);
    }

    public <K> DataContainerConfigurationBuilder evictable(Predicate<K> evictable) {
        this.attributes.attribute(EVICTABLE_PREDICATE).set((evictable != null) ? evictable : EVICTABLE_PREDICATE.getDefaultValue());
        return this;
    }

    @Override
    public void validate() {
    }

    @Override
    public DataContainerConfiguration create() {
        return new DataContainerConfiguration(this.attributes);
    }

    @Override
    public DataContainerConfigurationBuilder read(DataContainerConfiguration template, Combine combine) {
        this.attributes.read(template.attributes(), combine);
        return this;
    }

    @Override
    public AttributeSet attributes() {
        return this.attributes;
    }
}
