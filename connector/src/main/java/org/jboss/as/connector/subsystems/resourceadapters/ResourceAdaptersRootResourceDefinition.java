/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersExtension.SUBSYSTEM_NAME;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ResourceAdaptersRootResourceDefinition extends SimpleResourceDefinition {

    private final boolean runtimeOnlyRegistrationValid;

    public ResourceAdaptersRootResourceDefinition(boolean runtimeOnlyRegistrationValid) {
        super(ResourceAdaptersExtension.SUBSYSTEM_PATH, ResourceAdaptersExtension.getResourceDescriptionResolver(SUBSYSTEM_NAME), ResourceAdaptersSubsystemAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
        this.runtimeOnlyRegistrationValid = runtimeOnlyRegistrationValid;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ResourceAdapterResourceDefinition(false, runtimeOnlyRegistrationValid));
    }

    static void registerTransformers(SubsystemRegistration subsystem) {
        ResourceTransformationDescriptionBuilder builder110 = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceAdapterResourceDefinition.registerTransformers110(builder110);
        TransformationDescription.Tools.register(builder110.build(), subsystem, ModelVersion.create(1, 1, 0));
        ResourceTransformationDescriptionBuilder builder120 = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceAdapterResourceDefinition.registerTransformers120(builder120);
        TransformationDescription.Tools.register(builder120.build(), subsystem, ModelVersion.create(1, 2, 0));
        // Apply same to RBAC-updated version
        TransformationDescription.Tools.register(builder120.build(), subsystem, ModelVersion.create(1, 3, 0));
        ResourceTransformationDescriptionBuilder builder200 = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceAdapterResourceDefinition.registerTransformers200(builder200);
        TransformationDescription.Tools.register(builder120.build(), subsystem, ModelVersion.create(2, 0, 0));

    }
}
