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
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for the timer-service resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class TimerServiceResourceDefinition extends SimpleResourceDefinition {

    public static final TimerServiceResourceDefinition INSTANCE = new TimerServiceResourceDefinition();

    public static final SimpleAttributeDefinition PATH =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.PATH, ModelType.STRING, true)
                    .setValidator(new ModelTypeValidator(ModelType.STRING, true, false))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.RELATIVE_TO, ModelType.STRING, true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    public static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING, false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();


    public static final Map<String, AttributeDefinition> ATTRIBUTES ;

    static {
        Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        map.put(PATH.getName(), PATH);
        map.put(RELATIVE_TO.getName(), RELATIVE_TO);
        map.put(THREAD_POOL_NAME.getName(), THREAD_POOL_NAME);

        ATTRIBUTES = Collections.unmodifiableMap(map);
    }

    private TimerServiceResourceDefinition() {
        super(EJB3SubsystemModel.TIMER_SERVICE_PATH,
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.TIMER_SERVICE),
                TimerServiceAdd.INSTANCE, TimerServiceRemove.INSTANCE,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES.values()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }
}
