/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.session;

import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.modules.Module;

/**
 * Simple {@link DistributableSessionManagerConfiguration} implementation that delegates to {@link JBossWebMetaData}.
 * @author Paul Ferraro
 */
public class SimpleDistributableSessionManagerConfiguration implements DistributableSessionManagerConfiguration {

    private final Integer maxActiveSessions;
    private final ReplicationConfig replicationConfig;
    private final String serverName;
    private final String deploymentName;
    private final Module module;

    public SimpleDistributableSessionManagerConfiguration(JBossWebMetaData metaData, String serverName, String deploymentName, Module module) {
        this(metaData.getMaxActiveSessions(), metaData.getReplicationConfig(), serverName, deploymentName, module);
    }

    public SimpleDistributableSessionManagerConfiguration(SharedSessionManagerConfig config, String serverName, String deploymentName, Module module) {
        this(config.getMaxActiveSessions(), config.getReplicationConfig(), serverName, deploymentName, module);
    }

    public SimpleDistributableSessionManagerConfiguration(Integer maxActiveSessions, ReplicationConfig replicationConfig, String serverName, String deploymentName, Module module) {
        this.maxActiveSessions = maxActiveSessions;
        this.replicationConfig = replicationConfig;
        this.serverName = serverName;
        this.deploymentName = deploymentName;
        this.module = module;
    }

    @Override
    public int getMaxActiveSessions() {
        return (this.maxActiveSessions != null) ? this.maxActiveSessions.intValue() : -1;
    }

    @Override
    public ReplicationGranularity getGranularity() {
        return ((this.replicationConfig != null) && (this.replicationConfig.getReplicationGranularity() != null)) ? this.replicationConfig.getReplicationGranularity() : ReplicationGranularity.SESSION;
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    @Override
    public String getDeploymentName() {
        return this.deploymentName;
    }

    @Override
    public Module getModule() {
        return this.module;
    }

    @Override
    public String getCacheName() {
        return (this.replicationConfig != null) ? this.replicationConfig.getCacheName() : null;
    }
}
