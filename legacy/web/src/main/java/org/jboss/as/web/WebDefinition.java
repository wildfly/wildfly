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

package org.jboss.as.web;

import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 22.2.12 14:29
 */
public class WebDefinition extends ModelOnlyResourceDefinition {
    protected static final SimpleAttributeDefinition DEFAULT_VIRTUAL_SERVER =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_VIRTUAL_SERVER, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("default-host"))
                    .build();
    protected static final SimpleAttributeDefinition NATIVE =
            new SimpleAttributeDefinitionBuilder(Constants.NATIVE, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .build();
    protected static final SimpleAttributeDefinition INSTANCE_ID =
            new SimpleAttributeDefinitionBuilder(Constants.INSTANCE_ID, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(null)
                    .build();
    protected static final SimpleAttributeDefinition DEFAULT_SESSION_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_SESSION_TIMEOUT, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(30))
                    .setMeasurementUnit(MeasurementUnit.MINUTES)
                    .build();
    public static final WebDefinition INSTANCE = new WebDefinition();

    private WebDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, WebExtension.SUBSYSTEM_NAME),
                WebExtension.getResourceDescriptionResolver(null),
                DEFAULT_VIRTUAL_SERVER, NATIVE, INSTANCE_ID, DEFAULT_SESSION_TIMEOUT);
                setDeprecated(WebExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        WebMigrateOperation.registerOperations(resourceRegistration, getResourceDescriptionResolver());
    }
}
