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

package org.jboss.as.patching.management;

import org.jboss.as.boot.DirectoryStructure;
import org.jboss.as.controller.ModelControllerServiceInitialization;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.version.ProductConfig;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 */
public final class PatchIntegrationFactory implements ModelControllerServiceInitialization {

    static final ServiceName JBOSS_AS = ServiceName.JBOSS.append("as");
    static final ServiceName JBOSS_DIRECTORY_STRUCTURE_SERVICE = JBOSS_AS.append("directory-structure");
    static final ServiceName JBOSS_PRODUCT_CONFIG_SERVICE = JBOSS_AS.append("product-config");

    @Override
    public void initializeStandalone(final ServiceTarget serviceTarget, final ManagementResourceRegistration registration, final Resource resource) {
        initializeCoreServices(serviceTarget, registration, resource);
    }

    @Override
    public void initializeHost(final ServiceTarget serviceTarget, final ManagementResourceRegistration registration, final Resource resource) {
        initializeCoreServices(serviceTarget, registration, resource);
    }

    protected void initializeCoreServices(final ServiceTarget serviceTarget, final ManagementResourceRegistration registration, final Resource resource) {

        // Install the patch service
        final PatchInfoService service = new PatchInfoService();
        serviceTarget.addService(PatchInfoService.NAME, service)
                .addDependency(JBOSS_DIRECTORY_STRUCTURE_SERVICE, DirectoryStructure.class, service.getDirectoryStructure())
                .addDependency(JBOSS_PRODUCT_CONFIG_SERVICE, ProductConfig.class, service.getProductConfig())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();

        // Register the patch resource description
        registration.registerSubModel(PatchResourceDefinition.INSTANCE);
        // and resource
        resource.registerChild(PatchResourceDefinition.PATH, Resource.Factory.create());
    }

    @Override
    public void initializeDomain(final ServiceTarget serviceTarget, final ManagementResourceRegistration registration, final Resource resource) {
        // Nothing required here
    }

}
