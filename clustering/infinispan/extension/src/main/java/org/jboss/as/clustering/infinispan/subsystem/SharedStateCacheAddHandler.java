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

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Paul Ferraro
 */
public abstract class SharedStateCacheAddHandler extends ClusteredCacheAddHandler {

    SharedStateCacheAddHandler(CacheMode mode) {
        super(mode);
    }

    @Override
    void processModelNode(OperationContext context, String containerName, ModelNode containerModel, ModelNode cache, AdvancedCacheConfigurationBuilder configBuilder) throws OperationFailedException {

        // process the basic clustered configuration
        super.processModelNode(context, containerName, containerModel, cache, configBuilder);

        ConfigurationBuilder builder = configBuilder.getConfigurationBuilder();

        if (cache.hasDefined(StateTransferResourceDefinition.PATH.getKey())) {
            ModelNode stateTransfer = cache.get(StateTransferResourceDefinition.PATH.getKeyValuePair());
            if (stateTransfer.isDefined()) {
                builder.clustering().stateTransfer()
                        .fetchInMemoryState(StateTransferResourceDefinition.ENABLED.resolveModelAttribute(context, stateTransfer).asBoolean())
                        .timeout(StateTransferResourceDefinition.TIMEOUT.resolveModelAttribute(context, stateTransfer).asLong())
                        .chunkSize(StateTransferResourceDefinition.CHUNK_SIZE.resolveModelAttribute(context, stateTransfer).asInt())
                ;
            }
        }

        if (cache.hasDefined(BackupSiteResourceDefinition.WILDCARD_PATH.getKey())) {
            for (Property property: cache.get(BackupSiteResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                String siteName = property.getName();
                ModelNode site = property.getValue();
                builder.sites().addBackup()
                        .site(siteName)
                        .backupFailurePolicy(BackupFailurePolicy.valueOf(BackupSiteResourceDefinition.FAILURE_POLICY.resolveModelAttribute(context, site).asString()))
                        .strategy(BackupStrategy.valueOf(BackupSiteResourceDefinition.STRATEGY.resolveModelAttribute(context, site).asString()))
                        .replicationTimeout(BackupSiteResourceDefinition.REPLICATION_TIMEOUT.resolveModelAttribute(context, site).asLong())
                ;
                if (BackupSiteResourceDefinition.ENABLED.resolveModelAttribute(context, site).asBoolean()) {
                    builder.sites().addInUseBackupSite(siteName);
                }
            }
        }

        if (cache.hasDefined(BackupForResourceDefinition.PATH.getKey())) {
            ModelNode backupFor = cache.get(BackupForResourceDefinition.PATH.getKeyValuePair());
            if (backupFor.isDefined()) {
                builder.sites().backupFor()
                        .remoteCache(ModelNodes.asString(BackupForResourceDefinition.REMOTE_CACHE.resolveModelAttribute(context, backupFor)))
                        .remoteSite(ModelNodes.asString(BackupForResourceDefinition.REMOTE_SITE.resolveModelAttribute(context, backupFor)))
                ;
            }
        }
    }
}
