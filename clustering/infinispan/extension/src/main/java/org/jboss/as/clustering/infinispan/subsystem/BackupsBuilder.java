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
import static org.jboss.as.clustering.infinispan.subsystem.BackupResourceDefinition.TakeOfflineAttribute.*;

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
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public class BackupsBuilder extends ComponentBuilder<SitesConfiguration> implements ResourceServiceBuilder<SitesConfiguration> {

    private final InjectedValue<BackupForConfiguration> backupFor = new InjectedValue<>();
    private final Map<String, BackupConfiguration> backups = new HashMap<>();

    private final PathAddress cacheAddress;

    BackupsBuilder(PathAddress cacheAddress) {
        super(CacheComponent.BACKUPS, cacheAddress);
        this.cacheAddress = cacheAddress;
    }

    @Override
    public ServiceBuilder<SitesConfiguration> build(ServiceTarget target) {
        return super.build(target).addDependency(CacheComponent.BACKUP_FOR.getServiceName(this.cacheAddress), BackupForConfiguration.class, this.backupFor);
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
                        .enabled(ENABLED.resolveModelAttribute(context, backup).asBoolean())
                        .backupFailurePolicy(ModelNodes.asEnum(FAILURE_POLICY.resolveModelAttribute(context, backup), BackupFailurePolicy.class))
                        .replicationTimeout(TIMEOUT.resolveModelAttribute(context, backup).asLong())
                        .strategy(ModelNodes.asEnum(STRATEGY.resolveModelAttribute(context, backup), BackupStrategy.class))
                        .takeOffline()
                            .afterFailures(AFTER_FAILURES.resolveModelAttribute(context, backup).asInt())
                            .minTimeToWait(MIN_WAIT.resolveModelAttribute(context, backup).asLong())
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
