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

import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/invalidation-cache=*
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InvalidationCacheResourceDefinition extends ClusteredCacheResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static final PathElement pathElement(String name) {
        return PathElement.pathElement("invalidation-cache", name);
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);

        ClusteredCacheResourceDefinition.buildTransformation(version, builder);
    }

    InvalidationCacheResourceDefinition(PathManager pathManager, boolean allowRuntimeOnlyRegistration) {
        super(WILDCARD_PATH, pathManager, allowRuntimeOnlyRegistration);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(ClusteredCacheResourceDefinition.Attribute.class)
                .addAttributes(ClusteredCacheResourceDefinition.DeprecatedAttribute.class)
                .addAttributes(CacheResourceDefinition.Attribute.class)
                .addAttributes(CacheResourceDefinition.DeprecatedAttribute.class)
                .addCapabilities(CacheResourceDefinition.Capability.class)
                .addRequiredChildren(EvictionResourceDefinition.PATH, ExpirationResourceDefinition.PATH, LockingResourceDefinition.PATH, TransactionResourceDefinition.PATH)
                .addRequiredSingletonChildren(NoStoreResourceDefinition.PATH)
                ;
        ResourceServiceHandler handler = new InvalidationCacheServiceHandler();
        new AddStepHandler(descriptor, handler).register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);

        super.register(registration);
    }
}
