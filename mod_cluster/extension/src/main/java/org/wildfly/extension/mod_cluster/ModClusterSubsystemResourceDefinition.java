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

package org.wildfly.extension.mod_cluster;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ModClusterSubsystemResourceDefinition extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME);

    public static final SimpleAttributeDefinition PORT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PORT, ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new IntRangeValidator(1, Short.MAX_VALUE - Short.MIN_VALUE, false, false))
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition HOST = SimpleAttributeDefinitionBuilder.create(CommonAttributes.HOST, ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition VIRTUAL_HOST = SimpleAttributeDefinitionBuilder.create(CommonAttributes.VIRTUAL_HOST, ModelType.STRING, false)
            .addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition CONTEXT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CONTEXT, ModelType.STRING, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition WAIT_TIME = SimpleAttributeDefinitionBuilder.create(CommonAttributes.WAIT_TIME, ModelType.INT, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setStorageRuntime()
            .setDefaultValue(new ModelNode(10))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .build();

    private final boolean runtimeOnly;

    static TransformationDescription buildTransformation(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        ModClusterConfigResourceDefinition.buildTransformation(version, builder);

        return builder.build();
    }

    protected ModClusterSubsystemResourceDefinition(boolean runtimeOnly) {
        super(PATH,
                ModClusterExtension.getResourceDescriptionResolver(),
                ModClusterSubsystemAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler()
        );
        this.runtimeOnly = runtimeOnly;
    }

    /**
     * {@inheritDoc}
     * Registers an add operation handler or a remove operation handler if one was provided to the constructor.
     */
    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        if (runtimeOnly) {
            registerRuntimeOperations(registration);
        }

    }

    public void registerRuntimeOperations(ManagementResourceRegistration registration) {

        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();

        registration.registerOperationHandler(ModClusterListProxies.getDefinition(rootResolver), ModClusterListProxies.INSTANCE);

        registration.registerOperationHandler(ModClusterGetProxyInfo.getDefinition(rootResolver), ModClusterGetProxyInfo.INSTANCE);

        registration.registerOperationHandler(ModClusterGetProxyConfiguration.getDefinition(rootResolver), ModClusterGetProxyConfiguration.INSTANCE);

        // add/remove a proxy from the proxy-list (it is not persisted operation).
        registration.registerOperationHandler(ModClusterAddProxy.getDefinition(rootResolver), ModClusterAddProxy.INSTANCE);

        registration.registerOperationHandler(ModClusterRemoveProxy.getDefinition(rootResolver), ModClusterRemoveProxy.INSTANCE);

        // node related operations.
        registration.registerOperationHandler(ModClusterRefresh.getDefinition(rootResolver), ModClusterRefresh.INSTANCE);

        registration.registerOperationHandler(ModClusterReset.getDefinition(rootResolver), ModClusterReset.INSTANCE);

        // node (all contexts) related operations.
        registration.registerOperationHandler(ModClusterEnable.getDefinition(rootResolver), ModClusterEnable.INSTANCE);

        registration.registerOperationHandler(ModClusterDisable.getDefinition(rootResolver), ModClusterDisable.INSTANCE);

        registration.registerOperationHandler(ModClusterStop.getDefinition(rootResolver), ModClusterStop.INSTANCE);

        // Context related operations.
        registration.registerOperationHandler(ModClusterEnableContext.getDefinition(rootResolver), ModClusterEnableContext.INSTANCE);

        registration.registerOperationHandler(ModClusterDisableContext.getDefinition(rootResolver), ModClusterDisableContext.INSTANCE);

        registration.registerOperationHandler(ModClusterStopContext.getDefinition(rootResolver), ModClusterStopContext.INSTANCE);
    }


}
