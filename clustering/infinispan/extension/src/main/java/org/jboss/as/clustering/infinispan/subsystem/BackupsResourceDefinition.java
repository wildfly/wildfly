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

import org.infinispan.configuration.cache.SitesConfiguration;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ParentResourceServiceHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Definition of a backups resource.
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/component=backups
 *
 * @author Paul Ferraro
 */
public class BackupsResourceDefinition extends ComponentResourceDefinition {

    static final PathElement PATH = pathElement("backups");

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        PathAddressTransformer addressTransformer = new PathAddressTransformer() {
            @Override
            public PathAddress transform(PathElement current, Builder builder) {
                return builder.next();
            }
        };
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, addressTransformer) : parent.addChildResource(PATH);

        BackupResourceDefinition.buildTransformation(version, builder);
    }

    private final ResourceServiceBuilderFactory<SitesConfiguration> builderFactory = address -> new BackupsBuilder(address.getParent());

    public BackupsResourceDefinition() {
        super(PATH);
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver());
        ResourceServiceHandler handler = new ParentResourceServiceHandler<>(this.builderFactory);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        new BackupResourceDefinition(this.builderFactory).register(registration);
    }
}
