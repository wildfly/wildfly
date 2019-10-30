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

package org.jboss.as.appclient.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the App Client subsystem's root management resource.
 * <p/>
 * Note that in normal circumstances the app client subsystem will never be installed into a container that
 * provides access to the management API
 *
 * @author Stuart Douglas
 */
public class AppClientSubsystemResourceDefinition extends SimpleResourceDefinition {

    public static final AppClientSubsystemResourceDefinition INSTANCE = new AppClientSubsystemResourceDefinition();

    public static final SimpleAttributeDefinition FILE =
            new SimpleAttributeDefinitionBuilder(Constants.FILE, ModelType.STRING, false)
                    .setAllowExpression(true).build();
    public static final SimpleAttributeDefinition DEPLOYMENT =
            new SimpleAttributeDefinitionBuilder(Constants.DEPLOYMENT, ModelType.STRING, true)
                    .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition HOST_URL =
            new SimpleAttributeDefinitionBuilder(Constants.HOST_URL, ModelType.STRING, true)
                    .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition CONNECTION_PROPERTIES_URL =
            new SimpleAttributeDefinitionBuilder(Constants.CONNECTION_PROPERTIES_URL, ModelType.STRING, true)
                    .setAllowExpression(true).build();

    public static final StringListAttributeDefinition PARAMETERS = new StringListAttributeDefinition.Builder(Constants.PARAMETERS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    private AppClientSubsystemResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, AppClientExtension.SUBSYSTEM_NAME),
                AppClientExtension.getResourceDescriptionResolver(),
                AppClientSubsystemAdd.INSTANCE, null,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
    }

    static final AttributeDefinition[] ATTRIBUTES = {
            FILE,
            DEPLOYMENT,
            PARAMETERS,
            CONNECTION_PROPERTIES_URL,
            HOST_URL,
    };

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attr, null);
        }
    }
}
