/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.Timer;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.timer.TimerManagementProvider;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the timer-service resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class TimerServiceResourceDefinition extends SimpleResourceDefinition {

    public static final NullaryServiceDescriptor<Timer> TIMER_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.ejb3.timer-service", Timer.class);
    static final RuntimeCapability<Void> TIMER_SERVICE_CAPABILITY = RuntimeCapability.Builder.of(TIMER_SERVICE_DESCRIPTOR).build();

    static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setRequired(true)
                    .setAlternatives(EJB3SubsystemModel.DEFAULT_TRANSIENT_TIMER_MANAGEMENT)
                    .setCapabilityReference(CapabilityReferenceRecorder.builder(TIMER_SERVICE_CAPABILITY, EJB3SubsystemRootResourceDefinition.EXECUTOR_SERVICE_DESCRIPTOR).build())
                    .build();

    static final SimpleAttributeDefinition DEFAULT_DATA_STORE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_DATA_STORE, ModelType.STRING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setRequired(true)
                    .setAlternatives(EJB3SubsystemModel.DEFAULT_PERSISTENT_TIMER_MANAGEMENT)
                    .setRequires(EJB3SubsystemModel.THREAD_POOL_NAME)
                    .setCapabilityReference(CapabilityReferenceRecorder.builder(TIMER_SERVICE_CAPABILITY, TimerPersistence.SERVICE_DESCRIPTOR).build())
                    .build();

    static final SimpleAttributeDefinition DEFAULT_PERSISTENT_TIMER_MANAGEMENT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_PERSISTENT_TIMER_MANAGEMENT, ModelType.STRING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setRequired(true)
                    .setAlternatives(EJB3SubsystemModel.DEFAULT_DATA_STORE)
                    .setCapabilityReference(CapabilityReferenceRecorder.builder(TIMER_SERVICE_CAPABILITY, TimerManagementProvider.SERVICE_DESCRIPTOR).build())
                    .build();

    static final SimpleAttributeDefinition DEFAULT_TRANSIENT_TIMER_MANAGEMENT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_TRANSIENT_TIMER_MANAGEMENT, ModelType.STRING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setRequired(true)
                    .setAlternatives(EJB3SubsystemModel.THREAD_POOL_NAME)
                    .setCapabilityReference(CapabilityReferenceRecorder.builder(TIMER_SERVICE_CAPABILITY, TimerManagementProvider.SERVICE_DESCRIPTOR).build())
                    .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { THREAD_POOL_NAME, DEFAULT_DATA_STORE, DEFAULT_PERSISTENT_TIMER_MANAGEMENT, DEFAULT_TRANSIENT_TIMER_MANAGEMENT };

    private final PathManager pathManager;

    public TimerServiceResourceDefinition(final PathManager pathManager) {
        super(new SimpleResourceDefinition.Parameters(EJB3SubsystemModel.TIMER_SERVICE_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.TIMER_SERVICE))
                .setAddHandler(new TimerServiceAdd())
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setCapabilities(TIMER_SERVICE_CAPABILITY));
        this.pathManager = pathManager;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, ReloadRequiredWriteAttributeHandler.INSTANCE);
        }
    }

    @Override
    public void registerChildren(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new FileDataStoreResourceDefinition(pathManager));
        resourceRegistration.registerSubModel(new DatabaseDataStoreResourceDefinition());
    }
}
