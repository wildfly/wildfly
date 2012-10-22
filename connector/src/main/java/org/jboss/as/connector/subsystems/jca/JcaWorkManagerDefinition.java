package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.threads.BoundedQueueThreadPoolResourceDefinition;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelType;

import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_LONG_RUNNING;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_SHORT_RUNNING;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JcaWorkManagerDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_WORK_MANAGER = PathElement.pathElement(WORKMANAGER);
    private final boolean registerRuntimeOnly;

    private JcaWorkManagerDefinition(final boolean registerRuntimeOnly) {
        super(PATH_WORK_MANAGER,
                JcaExtension.getResourceDescriptionResolver(PATH_WORK_MANAGER.getKey()),
                WorkManagerAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    public static JcaWorkManagerDefinition createInstance(final boolean registerRuntimeOnly) {
        return new JcaWorkManagerDefinition(registerRuntimeOnly);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (final WmParameters parameter : WmParameters.values()) {
            resourceRegistration.registerReadWriteAttribute(parameter.getAttribute(), null, new ReloadRequiredWriteAttributeHandler());
        }

    }

    @Override
        public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(BoundedQueueThreadPoolResourceDefinition.create(WORKMANAGER_SHORT_RUNNING, ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER, ThreadsServices.STANDARD_HANDOFF_EXECUTOR_RESOLVER,
                            ThreadsServices.EXECUTOR.append(WORKMANAGER_SHORT_RUNNING), registerRuntimeOnly));
        resourceRegistration.registerSubModel(BoundedQueueThreadPoolResourceDefinition.create(WORKMANAGER_LONG_RUNNING, ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER, ThreadsServices.STANDARD_HANDOFF_EXECUTOR_RESOLVER,
                        ThreadsServices.EXECUTOR.append(WORKMANAGER_LONG_RUNNING), registerRuntimeOnly));

        }

    public static enum WmParameters {
        NAME(SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING)
                .setAllowExpression(true)
                .setAllowNull(false)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("name")
                .build());


        private WmParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }
}
