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

import java.util.Locale;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.dmr.ModelNode;

/**
 * Defines the Infinispan subsystem and its addressable resources.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanExtension implements Extension, DescriptionProvider {

    static final String SUBSYSTEM_NAME = "infinispan";

    private static final PathElement containerPath = PathElement.pathElement(ModelKeys.CACHE_CONTAINER);
    private static final PathElement localCachePath = PathElement.pathElement(ModelKeys.LOCAL_CACHE);
    private static final PathElement invalidationCachePath = PathElement.pathElement(ModelKeys.INVALIDATION_CACHE);
    private static final PathElement replicatedCachePath = PathElement.pathElement(ModelKeys.REPLICATED_CACHE);
    private static final PathElement distributedCachePath = PathElement.pathElement(ModelKeys.DISTRIBUTED_CACHE);

    private static final InfinispanSubsystemAdd add = new InfinispanSubsystemAdd();
    private static final InfinispanSubsystemDescribe describe = new InfinispanSubsystemDescribe();
    private static final DescriptionProvider remove = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getSubsystemRemoveDescription(locale);
        }
    };
    private static final CacheContainerAdd containerAdd = new CacheContainerAdd();
    private static final CacheContainerRemove containerRemove = new CacheContainerRemove();

    private static final DescriptionProvider containerDescription = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getCacheContainerDescription(locale);
        }
    };
    private static final DescriptionProvider localCacheDescription = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getLocalCacheDescription(locale);
        }
    };
    private static final DescriptionProvider invalidationCacheDescription = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getInvalidationCacheDescription(locale);
        }
    };
    private static final DescriptionProvider replicatedCacheDescription = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getReplicatedCacheDescription(locale);
        }
    };
    private static final DescriptionProvider distributedCacheDescription = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getDistributedCacheDescription(locale);
        }
    };

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        subsystem.registerXMLElementWriter(InfinispanSubsystemParser_1_0.getInstance());

        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(this);
        registration.registerOperationHandler(ModelDescriptionConstants.ADD, add, add, false);
        registration.registerOperationHandler(ModelDescriptionConstants.DESCRIBE, describe, describe, false, EntryType.PRIVATE);
        registration.registerOperationHandler(ModelDescriptionConstants.REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, remove, false);

        ManagementResourceRegistration container = registration.registerSubModel(containerPath, containerDescription);
        container.registerOperationHandler(ModelDescriptionConstants.ADD, containerAdd, containerAdd, false);
        container.registerOperationHandler(ModelDescriptionConstants.REMOVE, containerRemove, containerRemove, false);

        // add /subsystem=infinispan/cache-container=*/local-cache=*
        ManagementResourceRegistration local = container.registerSubModel(localCachePath, localCacheDescription);
        local.registerOperationHandler(ModelDescriptionConstants.ADD, LocalCacheAdd.INSTANCE, LocalCacheAdd.INSTANCE, false);
        local.registerOperationHandler(ModelDescriptionConstants.REMOVE, CacheRemove.INSTANCE, CacheRemove.INSTANCE, false);

        // add /subsystem=infinispan/cache-container=*/invalidation-cache=*
        ManagementResourceRegistration invalidation = container.registerSubModel(invalidationCachePath, invalidationCacheDescription);
        invalidation.registerOperationHandler(ModelDescriptionConstants.ADD, InvalidationCacheAdd.INSTANCE, InvalidationCacheAdd.INSTANCE, false);
        invalidation.registerOperationHandler(ModelDescriptionConstants.REMOVE, CacheRemove.INSTANCE, CacheRemove.INSTANCE, false);

        // add /subsystem=infinispan/cache-container=*/local-cache=*
        ManagementResourceRegistration replicated = container.registerSubModel(replicatedCachePath, replicatedCacheDescription);
        replicated.registerOperationHandler(ModelDescriptionConstants.ADD, ReplicatedCacheAdd.INSTANCE, ReplicatedCacheAdd.INSTANCE, false);
        replicated.registerOperationHandler(ModelDescriptionConstants.REMOVE, CacheRemove.INSTANCE, CacheRemove.INSTANCE, false);

        // add /subsystem=infinispan/cache-container=*/local-cache=*
        ManagementResourceRegistration distributed = container.registerSubModel(distributedCachePath, distributedCacheDescription);
        distributed.registerOperationHandler(ModelDescriptionConstants.ADD, DistributedCacheAdd.INSTANCE, DistributedCacheAdd.INSTANCE, false);
        distributed.registerOperationHandler(ModelDescriptionConstants.REMOVE, CacheRemove.INSTANCE, CacheRemove.INSTANCE, false);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.INFINISPAN_1_0.getUri(), InfinispanSubsystemParser_1_0.getInstance());
        context.setSubsystemXmlMapping(Namespace.INFINISPAN_1_1.getUri(),InfinispanSubsystemParser_1_0.getInstance());
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.descriptions.DescriptionProvider#getModelDescription(java.util.Locale)
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getSubsystemDescription(locale);
    }

}
