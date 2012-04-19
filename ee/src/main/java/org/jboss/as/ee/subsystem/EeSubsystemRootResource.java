/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ee.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultResourceAddDescriptionProvider;
import org.jboss.as.controller.descriptions.DefaultResourceRemoveDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.ee.component.deployers.DefaultEarSubDeploymentsIsolationProcessor;
import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacementProcessor;
import org.jboss.as.ee.structure.GlobalModuleDependencyProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

/**
 * {@link ResourceDefinition} for the EE subsystem's root management resource.
 *
 * @author Stuart Douglas
 */
public class EeSubsystemRootResource extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition EAR_SUBDEPLOYMENTS_ISOLATED =
            new SimpleAttributeDefinition("ear-subdeployments-isolated", "ear-subdeployments-isolated",
                    new ModelNode().set(false), ModelType.BOOLEAN, true, true, null);

    public static final SimpleAttributeDefinition SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT = new SimpleAttributeDefinition("spec-descriptor-property-replacement",
            "spec-descriptor-property-replacement",  new ModelNode().set(false), ModelType.BOOLEAN, true, true, null);

    public static final EeSubsystemRootResource INSTANCE = new EeSubsystemRootResource();

    // Our different operation handlers manipulate the state of the subsystem's DUPs, so they need to share a ref
    private final DefaultEarSubDeploymentsIsolationProcessor isolationProcessor = new DefaultEarSubDeploymentsIsolationProcessor();
    private final GlobalModuleDependencyProcessor moduleDependencyProcessor = new GlobalModuleDependencyProcessor();
    private final SpecDescriptorPropertyReplacementProcessor specDescriptorPropertyReplacementProcessor = new SpecDescriptorPropertyReplacementProcessor();

    private EeSubsystemRootResource() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, EeExtension.SUBSYSTEM_NAME),
                EeExtension.getResourceDescriptionResolver(EeExtension.SUBSYSTEM_NAME));
    }

    @Override
    public void registerOperations(final ManagementResourceRegistration rootResourceRegistration) {

        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();

        // Ops to add and remove the root resource
        final EeSubsystemAdd subsystemAdd = new EeSubsystemAdd(isolationProcessor, moduleDependencyProcessor, specDescriptorPropertyReplacementProcessor);
        final DescriptionProvider subsystemAddDescription = new DefaultResourceAddDescriptionProvider(rootResourceRegistration, rootResolver);
        rootResourceRegistration.registerOperationHandler(ADD, subsystemAdd, subsystemAddDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
        final DescriptionProvider subsystemRemoveDescription = new DefaultResourceRemoveDescriptionProvider(rootResolver);
        rootResourceRegistration.registerOperationHandler(REMOVE, EeSubsystemRemove.INSTANCE, subsystemRemoveDescription, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));

    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration rootResourceRegistration) {
        EeWriteAttributeHandler writeHandler = new EeWriteAttributeHandler(isolationProcessor, moduleDependencyProcessor, specDescriptorPropertyReplacementProcessor);
        writeHandler.registerAttributes(rootResourceRegistration);
    }
}
