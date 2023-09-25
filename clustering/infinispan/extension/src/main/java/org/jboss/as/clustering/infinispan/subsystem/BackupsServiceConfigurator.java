/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.BackupResourceDefinition.Attribute.FAILURE_POLICY;
import static org.jboss.as.clustering.infinispan.subsystem.BackupResourceDefinition.Attribute.STRATEGY;
import static org.jboss.as.clustering.infinispan.subsystem.BackupResourceDefinition.Attribute.TIMEOUT;
import static org.jboss.as.clustering.infinispan.subsystem.BackupResourceDefinition.TakeOfflineAttribute.AFTER_FAILURES;
import static org.jboss.as.clustering.infinispan.subsystem.BackupResourceDefinition.TakeOfflineAttribute.MIN_WAIT;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.configuration.cache.BackupConfiguration;
import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.infinispan.configuration.cache.BackupConfigurationBuilder;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.infinispan.configuration.cache.SitesConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class BackupsServiceConfigurator extends ComponentServiceConfigurator<SitesConfiguration> {

    private final Map<String, BackupConfiguration> backups = new HashMap<>();

    BackupsServiceConfigurator(PathAddress address) {
        super(CacheComponent.BACKUPS, address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.backups.clear();
        if (model.hasDefined(BackupResourceDefinition.WILDCARD_PATH.getKey())) {
            SitesConfigurationBuilder builder = new ConfigurationBuilder().sites();
            for (Property property : model.get(BackupResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                String siteName = property.getName();
                ModelNode backup = property.getValue();
                BackupConfigurationBuilder backupBuilder = builder.addBackup();
                backupBuilder.site(siteName)
                        .backupFailurePolicy(BackupFailurePolicy.valueOf(FAILURE_POLICY.resolveModelAttribute(context, backup).asString()))
                        .replicationTimeout(TIMEOUT.resolveModelAttribute(context, backup).asLong())
                        .strategy(BackupStrategy.valueOf(STRATEGY.resolveModelAttribute(context, backup).asString()))
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
    public SitesConfiguration get() {
        SitesConfigurationBuilder builder = new ConfigurationBuilder().sites();
        for (Map.Entry<String, BackupConfiguration> backup : this.backups.entrySet()) {
            builder.addBackup().read(backup.getValue());
        }
        return builder.create();
    }
}
