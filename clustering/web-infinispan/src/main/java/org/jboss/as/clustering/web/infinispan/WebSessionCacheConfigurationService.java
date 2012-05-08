/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.web.infinispan;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.subsystem.AbstractCacheConfigurationService;
import org.jboss.msc.value.Value;

/**
 * @author Paul Ferraro
 */
public class WebSessionCacheConfigurationService extends AbstractCacheConfigurationService {

    private final Value<Configuration> configuration;
    private final Value<EmbeddedCacheManager> container;

    public WebSessionCacheConfigurationService(String name, Value<EmbeddedCacheManager> container, Value<Configuration> configuration) {
        super(name);
        this.configuration = configuration;
        this.container = container;
    }

    @Override
    protected ConfigurationBuilder getConfigurationBuilder() {
        ConfigurationBuilder builder = new ConfigurationBuilder().read(this.configuration.getValue());
        builder.transaction().syncCommitPhase(false);
        return builder;
    }

    @Override
    protected EmbeddedCacheManager getCacheContainer() {
        return this.container.getValue();
    }
}
