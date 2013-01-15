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
package org.jboss.as.osgi.parser;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.osgi.management.ActivateOperationHandler;
import org.jboss.as.osgi.management.ActivationAttributeHandler;
import org.jboss.as.osgi.management.StartLevelHandler;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class OSGiRootResource extends SimpleResourceDefinition {

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, OSGiExtension.SUBSYSTEM_NAME);

    private static final ResourceDescriptionResolver RESOLVER = OSGiResolvers.getResolver(OSGiExtension.SUBSYSTEM_NAME);
    public static final SimpleAttributeDefinition ACTIVATION = new SimpleAttributeDefinitionBuilder(ModelConstants.ACTIVATION, ModelType.STRING, false)
            .setDefaultValue(new ModelNode(SubsystemState.DEFAULT_ACTIVATION.toString()))
            .setValidator(new EnumValidator<Activation>(Activation.class, false, false))
            .setAllowExpression(true)
            .addFlag(Flag.RESTART_JVM)
            .build();
    public static final SimpleAttributeDefinition STARTLEVEL = new SimpleAttributeDefinitionBuilder(ModelConstants.STARTLEVEL, ModelType.INT, true)
            .setStorageRuntime()
            .addFlag(Flag.RESTART_NONE)
            .build();
    static final OperationDefinition ACTIVATE = new SimpleOperationDefinitionBuilder(ModelConstants.ACTIVATE, RESOLVER)
            .withFlag(OperationEntry.Flag.RESTART_NONE)
            .build();
    final boolean registerRuntimeOnly;

    OSGiRootResource(final boolean registerRuntimeOnly) {
        super(SUBSYSTEM_PATH, RESOLVER,
                OSGiSubsystemAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(ACTIVATION, null, ActivationAttributeHandler.INSTANCE);
        if (registerRuntimeOnly) {
            resourceRegistration.registerReadWriteAttribute(STARTLEVEL, StartLevelHandler.READ_HANDLER, StartLevelHandler.WRITE_HANDLER);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        if (registerRuntimeOnly) {
            resourceRegistration.registerOperationHandler(ACTIVATE, ActivateOperationHandler.INSTANCE);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new FrameworkPropertyResource());
        resourceRegistration.registerSubModel(new FrameworkCapabilityResource());
        if (registerRuntimeOnly) {
            resourceRegistration.registerSubModel(new BundleResource());
        }
    }
}
