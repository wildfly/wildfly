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

import java.util.function.Predicate;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationInfo;
import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.commons.configuration.attributes.IdentityAttributeCopier;
import org.infinispan.commons.configuration.attributes.Matchable;

/**
 * @author Paul Ferraro
 */
@BuiltBy(DataContainerConfigurationBuilder.class)
public class DataContainerConfiguration implements Matchable<DataContainerConfiguration>, ConfigurationInfo {
    private static final Predicate<Object> ALWAYS = new Predicate<Object>() {
        @Override
        public boolean test(Object key) {
            return true;
        }
    };

    @SuppressWarnings("rawtypes")
    static final AttributeDefinition<Predicate> EVICTABLE_PREDICATE = AttributeDefinition.builder("evictable", ALWAYS, Predicate.class)
            .copier(IdentityAttributeCopier.INSTANCE)
            .immutable()
            .build();

    @SuppressWarnings("rawtypes")
    private final Attribute<Predicate> evictable;

    DataContainerConfiguration(AttributeSet attributes) {
        this.evictable = attributes.attribute(EVICTABLE_PREDICATE);
    }

    public <K> Predicate<K> evictable() {
        return this.evictable.get();
    }

    @Override
    public boolean matches(DataContainerConfiguration configuration) {
        return this.evictable() == configuration.evictable();
    }
}
