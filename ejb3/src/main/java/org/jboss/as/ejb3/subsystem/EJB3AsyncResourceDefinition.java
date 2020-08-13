/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
 * A {@link org.jboss.as.controller.ResourceDefinition} for the EJB async service
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
    public static final EJB3AsyncResourceDefinition INSTANCE = new EJB3AsyncResourceDefinition();

    private EJB3AsyncResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(EJB3SubsystemModel.ASYNC_SERVICE_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.ASYNC))
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
