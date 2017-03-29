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

import java.util.function.Consumer;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.transform.RequiredChildResourceDiscardPolicy;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Base class for cache resources which require common cache attributes, clustered cache attributes
 * and shared cache attributes.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class SharedStateCacheResourceDefinition extends ClusteredCacheResourceDefinition {

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        StateTransferResourceDefinition.buildTransformation(version, builder);

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.addChildResource(PartitionHandlingResourceDefinition.PATH, RequiredChildResourceDiscardPolicy.REJECT_AND_WARN);
        } else {
            PartitionHandlingResourceDefinition.buildTransformation(version, builder);
        }

        if (InfinispanModel.VERSION_2_0_0.requiresTransformation(version)) {
            final ResourceTransformationDescriptionBuilder backupsBuilder = builder.addChildResource(BackupsResourceDefinition.PATH, RequiredChildResourceDiscardPolicy.REJECT_AND_WARN);
            backupsBuilder.rejectChildResource(BackupResourceDefinition.WILDCARD_PATH);

            builder.addChildResource(BackupForResourceDefinition.PATH, RequiredChildResourceDiscardPolicy.REJECT_AND_WARN);
        } else {
            BackupsResourceDefinition.buildTransformation(version, builder);
            BackupForResourceDefinition.buildTransformation(version, builder);
        }

        ClusteredCacheResourceDefinition.buildTransformation(version, builder);
    }

    SharedStateCacheResourceDefinition(PathElement path, Consumer<ResourceDescriptor> descriptorConfigurator, ClusteredCacheServiceHandler handler) {
        super(path, descriptorConfigurator.andThen(descriptor -> descriptor
                .addRequiredChildren(PartitionHandlingResourceDefinition.PATH, StateTransferResourceDefinition.PATH, BackupForResourceDefinition.PATH, BackupsResourceDefinition.PATH)
            ), handler, registration -> {
                new PartitionHandlingResourceDefinition().register(registration);
                new StateTransferResourceDefinition().register(registration);
                new BackupsResourceDefinition().register(registration);
                new BackupForResourceDefinition().register(registration);
            });
    }
}
