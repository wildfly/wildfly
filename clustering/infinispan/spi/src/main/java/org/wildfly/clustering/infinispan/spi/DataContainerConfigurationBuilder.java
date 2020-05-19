/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.infinispan.spi;

import static org.wildfly.clustering.infinispan.spi.DataContainerConfiguration.*;

import java.util.function.Predicate;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.ConfigurationBuilderInfo;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.ConfigurationBuilder;

/**
 * @author Paul Ferraro
 */
public class DataContainerConfigurationBuilder implements Builder<DataContainerConfiguration>, ConfigurationBuilderInfo {

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
    public DataContainerConfigurationBuilder read(DataContainerConfiguration template) {
        return this.evictable(template.evictable());
    }
}
