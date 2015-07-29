/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.BackupResourceDefinition.Attribute.*;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.BackupConfigurationBuilder;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.BackupForConfiguration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.infinispan.configuration.cache.SitesConfigurationBuilder;
import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public class BackupsBuilder extends CacheComponentBuilder<SitesConfiguration> implements ResourceServiceBuilder<SitesConfiguration> {

    private final InjectedValue<BackupForConfiguration> backupFor = new InjectedValue<>();
    private final Map<String, BackupConfiguration> backups = new HashMap<>();

    private final String containerName;
    private final String cacheName;

    BackupsBuilder(String containerName, String cacheName) {
        super(CacheComponent.BACKUPS, containerName, cacheName);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceBuilder<SitesConfiguration> build(ServiceTarget target) {
        return super.build(target).addDependency(CacheComponent.BACKUP_FOR.getServiceName(this.containerName, this.cacheName), BackupForConfiguration.class, this.backupFor);
    }

    @Override
    public Builder<SitesConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.backups.clear();
        if (model.hasDefined(BackupResourceDefinition.WILDCARD_PATH.getKey())) {
            SitesConfigurationBuilder builder = new ConfigurationBuilder().sites();
            for (Property property : model.get(BackupResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                String siteName = property.getName();
                ModelNode backup = property.getValue();
                BackupConfigurationBuilder backupBuilder = builder.addBackup();
                backupBuilder.site(siteName)
                        .enabled(ENABLED.getDefinition().resolveModelAttribute(context, backup).asBoolean())
                        .backupFailurePolicy(ModelNodes.asEnum(FAILURE_POLICY.getDefinition().resolveModelAttribute(context, backup), BackupFailurePolicy.class))
                        .replicationTimeout(TIMEOUT.getDefinition().resolveModelAttribute(context, backup).asLong())
                        .strategy(ModelNodes.asEnum(STRATEGY.getDefinition().resolveModelAttribute(context, backup), BackupStrategy.class))
                        .takeOffline()
                            .afterFailures(TAKE_OFFLINE_AFTER_FAILURES.getDefinition().resolveModelAttribute(context, backup).asInt())
                            .minTimeToWait(TAKE_OFFLINE_MIN_WAIT.getDefinition().resolveModelAttribute(context, backup).asLong())
                ;
                this.backups.put(siteName, backupBuilder.create());
            }
        }
        return this;
    }

    @Override
    public SitesConfiguration getValue() {
        SitesConfigurationBuilder builder = new ConfigurationBuilder().sites();
        builder.backupFor().read(this.backupFor.getValue());
        builder.disableBackups(this.backups.isEmpty());
        for (Map.Entry<String, BackupConfiguration> backup : this.backups.entrySet()) {
            builder.addBackup().read(backup.getValue());
            builder.addInUseBackupSite(backup.getKey());
        }
        return builder.create();
    }
}
