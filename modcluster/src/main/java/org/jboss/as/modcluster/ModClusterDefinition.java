package org.jboss.as.modcluster;

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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

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
    public static final SimpleAttributeDefinition VIRUTAL_HOST = SimpleAttributeDefinitionBuilder.create(CommonAttributes.VIRUTAL_HOST, ModelType.STRING, false)
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

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        registration.registerReadWriteAttribute(ModClusterConfigResourceDefinition.getThisAttributeDefinition(), null, ModClusterConfigResourceDefinition.getThisAttributeHandler());
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

        //These seem to be modifying the state so don't add the runtimeOnly stuff for now
        final DescriptionProvider addProxy = new DefaultOperationDescriptionProvider(CommonAttributes.ADD_PROXY, rootResolver, HOST, PORT);
        registration.registerOperationHandler(CommonAttributes.ADD_PROXY, ModClusterAddProxy.INSTANCE, addProxy, false);

        final DescriptionProvider removeProxy = new DefaultOperationDescriptionProvider(CommonAttributes.REMOVE_PROXY, rootResolver, HOST, PORT);
        registration.registerOperationHandler(CommonAttributes.REMOVE_PROXY, ModClusterRemoveProxy.INSTANCE, removeProxy, false);

        // node related operations.
        final DescriptionProvider refresh = new DefaultOperationDescriptionProvider(CommonAttributes.REFRESH, rootResolver);
        registration.registerOperationHandler(CommonAttributes.REFRESH, ModClusterRefresh.INSTANCE, refresh, false, runtimeOnlyFlags);

        final DescriptionProvider reset = new DefaultOperationDescriptionProvider(CommonAttributes.RESET, rootResolver);
        registration.registerOperationHandler(CommonAttributes.RESET, ModClusterReset.INSTANCE, reset, false, runtimeOnlyFlags);

        // node (all contexts) related operations.
        final DescriptionProvider enable = new DefaultOperationDescriptionProvider(CommonAttributes.ENABLE, rootResolver);
        registration.registerOperationHandler(CommonAttributes.ENABLE, ModClusterEnable.INSTANCE, enable, false);

        final DescriptionProvider disable = new DefaultOperationDescriptionProvider(CommonAttributes.DISABLE, rootResolver);
        registration.registerOperationHandler(CommonAttributes.DISABLE, ModClusterDisable.INSTANCE, disable, false);

        final DescriptionProvider stop = new DefaultOperationDescriptionProvider(CommonAttributes.STOP, rootResolver);
        registration.registerOperationHandler(CommonAttributes.STOP, ModClusterStop.INSTANCE, stop, false);

        // Context related operations.
        final DescriptionProvider enableContext = new DefaultOperationDescriptionProvider(CommonAttributes.ENABLE_CONTEXT, rootResolver, VIRUTAL_HOST, CONTEXT);
        registration.registerOperationHandler(CommonAttributes.ENABLE_CONTEXT, ModClusterEnableContext.INSTANCE, enableContext, false);

        final DescriptionProvider disableContext = new DefaultOperationDescriptionProvider(CommonAttributes.DISABLE_CONTEXT, rootResolver, VIRUTAL_HOST, CONTEXT);
        registration.registerOperationHandler(CommonAttributes.DISABLE_CONTEXT, ModClusterDisableContext.INSTANCE, disableContext, false);

        final DescriptionProvider stopContext = new DefaultOperationDescriptionProvider(CommonAttributes.STOP_CONTEXT, rootResolver, VIRUTAL_HOST, CONTEXT, WAIT_TIME);
        registration.registerOperationHandler(CommonAttributes.STOP_CONTEXT, ModClusterStopContext.INSTANCE, stopContext, false);
    }


}
