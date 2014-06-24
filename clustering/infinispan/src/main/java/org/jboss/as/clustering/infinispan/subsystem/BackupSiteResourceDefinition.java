/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.xsite.XSiteAdminOperations;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author Paul Ferraro
 *
 */
public class BackupSiteResourceDefinition extends SimpleResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelKeys.BACKUP, name);
    }

    static final SimpleAttributeDefinition FAILURE_POLICY = new SimpleAttributeDefinitionBuilder(ModelKeys.BACKUP_FAILURE_POLICY, ModelType.STRING, true)
            .setXmlName(Attribute.BACKUP_FAILURE_POLICY.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(BackupFailurePolicy.class, true, true))
            .setDefaultValue(new ModelNode().set(BackupFailurePolicy.WARN.name()))
            .build();

    static final SimpleAttributeDefinition STRATEGY = new SimpleAttributeDefinitionBuilder(ModelKeys.BACKUP_STRATEGY, ModelType.STRING, true)
            .setXmlName(Attribute.STRATEGY.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(BackupStrategy.class, true, true))
            .setDefaultValue(new ModelNode().set(BackupStrategy.ASYNC.name()))
            .build();

    static final SimpleAttributeDefinition REPLICATION_TIMEOUT = new SimpleAttributeDefinitionBuilder(ModelKeys.TIMEOUT, ModelType.STRING, true)
            .setXmlName(Attribute.TIMEOUT.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(10000L))
            .build();

    static final SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder(ModelKeys.ENABLED, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.ENABLED.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(true))
            .build();

    static final SimpleAttributeDefinition TAKE_OFFLINE_AFTER_FAILURES = new SimpleAttributeDefinitionBuilder(ModelKeys.TAKE_BACKUP_OFFLINE_AFTER_FAILURES, ModelType.INT, true)
            .setXmlName(Attribute.TAKE_BACKUP_OFFLINE_AFTER_FAILURES.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(0))
            .build();

    static final SimpleAttributeDefinition TAKE_OFFLINE_MIN_WAIT = new SimpleAttributeDefinitionBuilder(ModelKeys.TAKE_BACKUP_OFFLINE_MIN_WAIT, ModelType.INT, true)
            .setXmlName(Attribute.TAKE_BACKUP_OFFLINE_MIN_WAIT.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(0))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { FAILURE_POLICY, STRATEGY, REPLICATION_TIMEOUT, ENABLED, TAKE_OFFLINE_AFTER_FAILURES, TAKE_OFFLINE_MIN_WAIT };

        // operations
    static final OperationDefinition BACKUP_BRING_SITE_ONLINE = new SimpleOperationDefinitionBuilder(ModelKeys.BRING_SITE_ONLINE, InfinispanExtension.getResourceDescriptionResolver("backup.ops"))
            .setRuntimeOnly()
            .build();

    static final OperationDefinition BACKUP_TAKE_SITE_OFFLINE = new SimpleOperationDefinitionBuilder(ModelKeys.TAKE_SITE_OFFLINE, InfinispanExtension.getResourceDescriptionResolver("backup.ops"))
            .setRuntimeOnly()
            .build();

    static final OperationDefinition BACKUP_SITE_STATUS = new SimpleOperationDefinitionBuilder(ModelKeys.SITE_STATUS, InfinispanExtension.getResourceDescriptionResolver("backup.ops"))
            .setRuntimeOnly()
            .setReadOnly()
            .build();

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);
    }

    private final boolean runtimeRegistration;

    BackupSiteResourceDefinition(final boolean runtimeRegistration) {
        super(WILDCARD_PATH, InfinispanExtension.getResourceDescriptionResolver(ModelKeys.BACKUP), new ReloadRequiredAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if (this.runtimeRegistration) {
            resourceRegistration.registerOperationHandler(BackupSiteResourceDefinition.BACKUP_BRING_SITE_ONLINE, new BackupBringSiteOnline());
            resourceRegistration.registerOperationHandler(BackupSiteResourceDefinition.BACKUP_TAKE_SITE_OFFLINE, new BackupTakeSiteOffline());
            resourceRegistration.registerOperationHandler(BackupSiteResourceDefinition.BACKUP_SITE_STATUS, new BackupSiteStatus());
        }
    }

    // operation handler definitions

    /*
     * Operation to bring a backup site online
     *
     *  backup=X:bring-site-online()
     *
     * where X is the name of the site.
     */
    class BackupBringSiteOnline extends AbstractRuntimeOnlyHandler {
        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
            //
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String cacheContainerName = address.getElement(address.size()-3).getValue();
            final String cacheName = address.getElement(address.size()-2).getValue();
            final String site = address.getLastElement().getValue();

            final ServiceName cacheServiceName = CacheService.getServiceName(cacheContainerName, cacheName);
            final ServiceController<?> controller = context.getServiceRegistry(true).getService(cacheServiceName);
            Cache<?, ?> cache = (Cache<?, ?>) controller.getValue();

            ModelNode result = null;
            try {
                XSiteAdminOperations xSiteAdminOperations = cache.getAdvancedCache().getComponentRegistry().getComponent(XSiteAdminOperations.class);
                String stringResult = xSiteAdminOperations.bringSiteOnline(site);
                result = new ModelNode().set(stringResult);
            } catch(Exception e) {
                throw InfinispanLogger.ROOT_LOGGER.failedToInvokeOperation(e.getCause(), "bring-site-online");
            }

            if (result != null) {
                context.getResult().set(result);
            }
            context.stepCompleted();
        }
    }

    /*
     * Operation to take a site offline
     *
     *  backup=X:take-site-offline()
     *
     * where X is the name of the site.
     */
    class BackupTakeSiteOffline extends AbstractRuntimeOnlyHandler {
        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
            //
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String cacheContainerName = address.getElement(address.size()-3).getValue();
            final String cacheName = address.getElement(address.size()-2).getValue();
            final String site = address.getLastElement().getValue();

            final ServiceName cacheServiceName = CacheService.getServiceName(cacheContainerName, cacheName);
            final ServiceController<?> controller = context.getServiceRegistry(true).getService(cacheServiceName);
            Cache<?, ?> cache = (Cache<?, ?>) controller.getValue();

            ModelNode result = null;
            try {
                XSiteAdminOperations xSiteAdminOperations = cache.getAdvancedCache().getComponentRegistry().getComponent(XSiteAdminOperations.class);
                String stringResult = xSiteAdminOperations.takeSiteOffline(site);
                result = new ModelNode().set(stringResult);
            } catch(Exception e) {
                throw InfinispanLogger.ROOT_LOGGER.failedToInvokeOperation(e.getCause(), "take-site-offline");
            }

            if (result != null) {
                context.getResult().set(result);
            }
            context.stepCompleted();
        }
    }

    /*
     * Operation to display the backup site status
     *
     *  backup=X:site-status
     *
     * where X is the name of the site.
     */
    class BackupSiteStatus extends AbstractRuntimeOnlyHandler {
        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
            //
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String cacheContainerName = address.getElement(address.size()-3).getValue();
            final String cacheName = address.getElement(address.size()-2).getValue();
            final String site = address.getLastElement().getValue();

            final ServiceName cacheServiceName = CacheService.getServiceName(cacheContainerName, cacheName);
            final ServiceController<?> controller = context.getServiceRegistry(true).getService(cacheServiceName);
            Cache<?, ?> cache = (Cache<?, ?>) controller.getValue();

            ModelNode result = null;
            try {
                XSiteAdminOperations xSiteAdminOperations = cache.getAdvancedCache().getComponentRegistry().getComponent(XSiteAdminOperations.class);
                String stringResult = xSiteAdminOperations.siteStatus(site);
                result = new ModelNode().set(stringResult);
            } catch(Exception e) {
                throw InfinispanLogger.ROOT_LOGGER.failedToInvokeOperation(e.getCause(), "site-status");
            }

            if (result != null) {
                context.getResult().set(result);
            }
            context.stepCompleted();
        }
    }
}
