/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.infinispan.subsystem.AbstractCacheConfigurationService;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.msc.value.Value;

/**
 * Web session cache configuration service.
 * @author Paul Ferraro
 */
public class SessionCacheConfigurationService extends AbstractCacheConfigurationService {
    private final Value<Configuration> configuration;
    private final Value<EmbeddedCacheManager> container;
    private final JBossWebMetaData metaData;

    public SessionCacheConfigurationService(String name, Value<EmbeddedCacheManager> container, Value<Configuration> configuration, JBossWebMetaData metaData) {
        super(name);
        this.configuration = configuration;
        this.container = container;
        this.metaData = metaData;
        ReplicationConfig config = this.metaData.getReplicationConfig();
        if (config == null) {
            config = new ReplicationConfig();
            this.metaData.setReplicationConfig(config);
        }
        ReplicationGranularity granularity = config.getReplicationGranularity();
        if (granularity == null) {
            config.setReplicationGranularity(ReplicationGranularity.SESSION);
        }
        Integer maxActiveSessions = this.metaData.getMaxActiveSessions();
        if (maxActiveSessions == null) {
            this.metaData.setMaxActiveSessions(Integer.valueOf(-1));
        }
    }

    @Override
    protected ConfigurationBuilder getConfigurationBuilder() {
        Configuration config = this.configuration.getValue();
        ConfigurationBuilder builder = new ConfigurationBuilder().read(config);
        builder.invocationBatching().enable();
        builder.storeAsBinary().disable().storeKeysAsBinary(false).storeValuesAsBinary(false);
        builder.locking().isolationLevel(IsolationLevel.REPEATABLE_READ);
        return builder;
    }

    @Override
    protected EmbeddedCacheManager getCacheContainer() {
        return this.container.getValue();
    }
}
