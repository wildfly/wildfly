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

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

import java.util.EnumSet;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class ModClusterDefinition extends SimpleResourceDefinition {
    public static final SimpleAttributeDefinition PORT = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PORT, ModelType.INT, false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
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
            .build();

    private final boolean runtimeOnly;


    protected ModClusterDefinition(boolean runtimeOnly) {
        super(ModClusterExtension.SUBSYSTEM_PATH,
                ModClusterExtension.getResourceDescriptionResolver(),
                ModClusterSubsystemAdd.INSTANCE,
                ModClusterSubsystemRemove.INSTANCE
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
        EnumSet<OperationEntry.Flag> runtimeOnlyFlags = EnumSet.of(OperationEntry.Flag.RUNTIME_ONLY);
        final ResourceDescriptionResolver rootResolver = getResourceDescriptionResolver();

        final DescriptionProvider listProxiesDescription = new DefaultOperationDescriptionProvider(CommonAttributes.LIST_PROXIES, rootResolver);
        registration.registerOperationHandler(CommonAttributes.LIST_PROXIES, ModClusterListProxies.INSTANCE, listProxiesDescription, false, runtimeOnlyFlags);

        final DescriptionProvider readProxiesInfoDescription = new DefaultOperationDescriptionProvider(CommonAttributes.READ_PROXIES_INFO, rootResolver);
        registration.registerOperationHandler(CommonAttributes.READ_PROXIES_INFO, ModClusterGetProxyInfo.INSTANCE, readProxiesInfoDescription, false, runtimeOnlyFlags);

        final DescriptionProvider readProxiesInfoConfiguration = new DefaultOperationDescriptionProvider(CommonAttributes.READ_PROXIES_CONFIGURATION, rootResolver);
        registration.registerOperationHandler(CommonAttributes.READ_PROXIES_CONFIGURATION, ModClusterGetProxyConfiguration.INSTANCE, readProxiesInfoConfiguration, false, runtimeOnlyFlags);

        // add/remove a proxy from the proxy-list (it is not persisted operation).
        final DescriptionProvider addProxy = new DefaultOperationDescriptionProvider(CommonAttributes.ADD_PROXY, rootResolver, HOST, PORT);
        registration.registerOperationHandler(CommonAttributes.ADD_PROXY, ModClusterAddProxy.INSTANCE, addProxy, false, runtimeOnlyFlags);

        final DescriptionProvider removeProxy = new DefaultOperationDescriptionProvider(CommonAttributes.REMOVE_PROXY, rootResolver, HOST, PORT);
        registration.registerOperationHandler(CommonAttributes.REMOVE_PROXY, ModClusterRemoveProxy.INSTANCE, removeProxy, false, runtimeOnlyFlags);

        // node related operations.
        final DescriptionProvider refresh = new DefaultOperationDescriptionProvider(CommonAttributes.REFRESH, rootResolver);
        registration.registerOperationHandler(CommonAttributes.REFRESH, ModClusterRefresh.INSTANCE, refresh, false, runtimeOnlyFlags);

        final DescriptionProvider reset = new DefaultOperationDescriptionProvider(CommonAttributes.RESET, rootResolver);
        registration.registerOperationHandler(CommonAttributes.RESET, ModClusterReset.INSTANCE, reset, false, runtimeOnlyFlags);

        // node (all contexts) related operations.
        final DescriptionProvider enable = new DefaultOperationDescriptionProvider(CommonAttributes.ENABLE, rootResolver);
        registration.registerOperationHandler(CommonAttributes.ENABLE, ModClusterEnable.INSTANCE, enable, false, runtimeOnlyFlags);

        final DescriptionProvider disable = new DefaultOperationDescriptionProvider(CommonAttributes.DISABLE, rootResolver);
        registration.registerOperationHandler(CommonAttributes.DISABLE, ModClusterDisable.INSTANCE, disable, false, runtimeOnlyFlags);

        final DescriptionProvider stop = new DefaultOperationDescriptionProvider(CommonAttributes.STOP, rootResolver);
        registration.registerOperationHandler(CommonAttributes.STOP, ModClusterStop.INSTANCE, stop, false, runtimeOnlyFlags);

        // Context related operations.
        final DescriptionProvider enableContext = new DefaultOperationDescriptionProvider(CommonAttributes.ENABLE_CONTEXT, rootResolver, VIRTUAL_HOST, CONTEXT);
        registration.registerOperationHandler(CommonAttributes.ENABLE_CONTEXT, ModClusterEnableContext.INSTANCE, enableContext, false, runtimeOnlyFlags);

        final DescriptionProvider disableContext = new DefaultOperationDescriptionProvider(CommonAttributes.DISABLE_CONTEXT, rootResolver, VIRTUAL_HOST, CONTEXT);
        registration.registerOperationHandler(CommonAttributes.DISABLE_CONTEXT, ModClusterDisableContext.INSTANCE, disableContext, false, runtimeOnlyFlags);

        final DescriptionProvider stopContext = new DefaultOperationDescriptionProvider(CommonAttributes.STOP_CONTEXT, rootResolver, VIRTUAL_HOST, CONTEXT, WAIT_TIME);
        registration.registerOperationHandler(CommonAttributes.STOP_CONTEXT, ModClusterStopContext.INSTANCE, stopContext, false, runtimeOnlyFlags);
    }


}
