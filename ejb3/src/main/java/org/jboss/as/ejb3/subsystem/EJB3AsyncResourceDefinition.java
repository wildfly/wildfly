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
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelType;

import java.util.concurrent.ExecutorService;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for the Jakarta Enterprise Beans async service
 * <p/>
 * @author Stuart Douglas
 */
public class EJB3AsyncResourceDefinition extends SimpleResourceDefinition {

    // this is an unregistered copy of the capability defined and registered in /subsystem=ejb3/thread-pool=*
    // needed due to the unorthodox way in which the thread pools are defined in ejb3 subsystem
    protected static final String THREAD_POOL_CAPABILITY_NAME = ThreadsServices.createCapability(EJB3SubsystemModel.BASE_EJB_THREAD_POOL_NAME, ExecutorService.class).getName();

    public static final String ASYNC_SERVICE_CAPABILITY_NAME = "org.wildfly.ejb3.async";
    public static final RuntimeCapability<Void> ASYNC_SERVICE_CAPABILITY =
            RuntimeCapability.Builder.of(ASYNC_SERVICE_CAPABILITY_NAME).build();

    static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setCapabilityReference(THREAD_POOL_CAPABILITY_NAME, ASYNC_SERVICE_CAPABILITY)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { THREAD_POOL_NAME };

    EJB3AsyncResourceDefinition() {
        super(new Parameters(EJB3SubsystemModel.ASYNC_SERVICE_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.ASYNC))
                .setAddHandler(new EJB3AsyncServiceAdd(ATTRIBUTES))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(ASYNC_SERVICE_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            // TODO: Make this RESTART_NONE by updating AsynchronousMergingProcessor
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }
}
