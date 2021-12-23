/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi.persistence.sifs;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.persistence.sifs.configuration.DataConfiguration;
import org.infinispan.persistence.sifs.configuration.IndexConfiguration;

/**
 * Workaround for ISPN-13605.
 * @author Paul Ferraro
 */
@BuiltBy(SoftIndexFileStoreConfigurationBuilder.class)
@ConfigurationFor(SoftIndexFileStore.class)
public class SoftIndexFileStoreConfiguration extends org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfiguration {

    public SoftIndexFileStoreConfiguration(AttributeSet attributes, AsyncStoreConfiguration async, IndexConfiguration indexConfiguration, DataConfiguration dataConfiguration) {
        super(attributes, async, indexConfiguration, dataConfiguration);
    }
}
