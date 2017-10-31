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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the timer-service resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class TimerServiceResourceDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING, false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static final SimpleAttributeDefinition DEFAULT_DATA_STORE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.DEFAULT_DATA_STORE, ModelType.STRING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setRequired(true)
                    //.setDefaultValue(new ModelNode("default-file-store")) //for backward compatibility!
                    .build();

    public static final Map<String, AttributeDefinition> ATTRIBUTES ;

    private final PathManager pathManager;

    static {
        Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        map.put(THREAD_POOL_NAME.getName(), THREAD_POOL_NAME);
        map.put(DEFAULT_DATA_STORE.getName(), DEFAULT_DATA_STORE);

        ATTRIBUTES = Collections.unmodifiableMap(map);
    }

    public TimerServiceResourceDefinition(final PathManager pathManager) {
        super(EJB3SubsystemModel.TIMER_SERVICE_PATH,
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.TIMER_SERVICE),
                TimerServiceAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
        this.pathManager = pathManager;
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES.values()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    @Override
    public void registerChildren(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new FileDataStoreResourceDefinition(pathManager));
        resourceRegistration.registerSubModel(DatabaseDataStoreResourceDefinition.INSTANCE);
    }

}
