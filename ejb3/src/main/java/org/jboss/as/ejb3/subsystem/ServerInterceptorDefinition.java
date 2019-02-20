/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
@author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
*/


public class ServerInterceptorDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.MODULE, ModelType.STRING, true).setRequired(true).setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition CLASS = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.CLASS, ModelType.STRING, true).setRequired(true).setAllowExpression(true)
            .build();

    public static final Map<String, AttributeDefinition> ATTRIBUTES;

    static {
        final Map<String, AttributeDefinition> map = new LinkedHashMap<String, AttributeDefinition>();
        map.put(MODULE.getName(), MODULE);
        map.put(CLASS.getName(), CLASS);

        ATTRIBUTES = Collections.unmodifiableMap(map);
    }

    public static final ServerInterceptorDefinition INSTANCE = new ServerInterceptorDefinition();

    private ServerInterceptorDefinition() {
        super(PathElement.pathElement(EJB3SubsystemModel.SERVER_INTERCEPTOR), EJB3Extension
                .getResourceDescriptionResolver(EJB3SubsystemModel.SERVER_INTERCEPTOR), ServerInterceptorAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (final AttributeDefinition attr : ATTRIBUTES.values()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new RemotingProfileResourceChildWriteAttributeHandler(attr));
        }
    }

}
