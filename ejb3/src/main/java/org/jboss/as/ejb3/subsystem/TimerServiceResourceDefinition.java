/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

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
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.timer.TimerServiceRequirement;

import java.util.Timer;
import java.util.concurrent.ExecutorService;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the timer-service resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class TimerServiceResourceDefinition extends SimpleResourceDefinition {

    // this is an unregistered copy of the capability defined and registered in /subsystem=ejb3/thread-pool=*
    // needed due to the unorthodox way in which the thread pools are defined in ejb3 subsystem
    public static final String THREAD_POOL_CAPABILITY_NAME = ThreadsServices.createCapability(EJB3SubsystemModel.BASE_EJB_THREAD_POOL_NAME, ExecutorService.class).getName();

    public static final String TIMER_PERSISTENCE_CAPABILITY_NAME = "org.wildfly.ejb3.timer-service.timer-persistence-service";

    public static final RuntimeCapability<Void> TIMER_PERSISTENCE_CAPABILITY =
            RuntimeCapability.Builder.of(TIMER_PERSISTENCE_CAPABILITY_NAME, true, TimerPersistence.class)
                    .setAllowMultipleRegistrations(true)
                    .build();

    public static final String TIMER_SERVICE_CAPABILITY_NAME = "org.wildfly.ejb3.timer-service";
    public static final RuntimeCapability<Void> TIMER_SERVICE_CAPABILITY = RuntimeCapability.Builder.of(TIMER_SERVICE_CAPABILITY_NAME, Timer.class).build();

    static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setRequired(true)
                    .setAlternatives(EJB3SubsystemModel.DEFAULT_TRANSIENT_TIMER_MANAGEMENT)
                    .setCapabilityReference(THREAD_POOL_CAPABILITY_NAME, TIMER_SERVICE_CAPABILITY)
                    .build();

    static final SimpleAttributeDefinition DEFAULT_DATA_STORE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_DATA_STORE, ModelType.STRING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setRequired(true)
                    .setAlternatives(EJB3SubsystemModel.DEFAULT_PERSISTENT_TIMER_MANAGEMENT)
                    .setRequires(EJB3SubsystemModel.THREAD_POOL_NAME)
                    .setCapabilityReference(TIMER_PERSISTENCE_CAPABILITY_NAME, TIMER_SERVICE_CAPABILITY)
                    .build();

    static final SimpleAttributeDefinition DEFAULT_PERSISTENT_TIMER_MANAGEMENT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_PERSISTENT_TIMER_MANAGEMENT, ModelType.STRING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setRequired(true)
                    .setAlternatives(EJB3SubsystemModel.DEFAULT_DATA_STORE)
                    .setCapabilityReference(TimerServiceRequirement.TIMER_MANAGEMENT_PROVIDER.getName(), TIMER_SERVICE_CAPABILITY)
                    .build();

    static final SimpleAttributeDefinition DEFAULT_TRANSIENT_TIMER_MANAGEMENT =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_TRANSIENT_TIMER_MANAGEMENT, ModelType.STRING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setRequired(true)
                    .setAlternatives(EJB3SubsystemModel.THREAD_POOL_NAME)
                    .setCapabilityReference(TimerServiceRequirement.TIMER_MANAGEMENT_PROVIDER.getName(), TIMER_SERVICE_CAPABILITY)
                    .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { THREAD_POOL_NAME, DEFAULT_DATA_STORE, DEFAULT_PERSISTENT_TIMER_MANAGEMENT, DEFAULT_TRANSIENT_TIMER_MANAGEMENT };

    private final PathManager pathManager;

    public TimerServiceResourceDefinition(final PathManager pathManager) {
        super(new SimpleResourceDefinition.Parameters(EJB3SubsystemModel.TIMER_SERVICE_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.TIMER_SERVICE))
                .setAddHandler(TimerServiceAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setCapabilities(TIMER_SERVICE_CAPABILITY));
        this.pathManager = pathManager;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    @Override
    public void registerChildren(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new FileDataStoreResourceDefinition(pathManager));
        resourceRegistration.registerSubModel(new DatabaseDataStoreResourceDefinition());
    }

}
