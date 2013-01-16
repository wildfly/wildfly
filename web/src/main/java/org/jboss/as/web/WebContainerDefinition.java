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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 18:03
 */
public class WebContainerDefinition extends SimpleResourceDefinition {
    public static final WebContainerDefinition INSTANCE = new WebContainerDefinition();

    protected static final SimpleListAttributeDefinition WELCOME_FILES =new SimpleListAttributeDefinition.Builder(Constants.WELCOME_FILE,
                    new SimpleAttributeDefinitionBuilder(Constants.WELCOME_FILE, ModelType.STRING, true)
                            .setXmlName(Constants.WELCOME_FILE)
                            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                            .setValidator(new StringLengthValidator(1, true))
                            .build())
                    .setAllowNull(true)
                    .build();
    protected static final PropertiesAttributeDefinition MIME_MAPPINGS = new PropertiesAttributeDefinition.Builder(Constants.MIME_MAPPING, true)
            .setAllowExpression(true)
            .build();

    private static final SimpleAttributeDefinition MIME_NAME = new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    private static final SimpleAttributeDefinition MIME_VALUE = new SimpleAttributeDefinitionBuilder(Constants.VALUE, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    protected static final AttributeDefinition[] CONTAINER_ATTRIBUTES={
            WELCOME_FILES,
            MIME_MAPPINGS,
    };
    private static final OperationDefinition ADD_MIME = new SimpleOperationDefinitionBuilder("add-mime", WebExtension.getResourceDescriptionResolver("container.mime-mapping"))
            .setParameters(MIME_NAME, MIME_VALUE)
            .build();
    private static final OperationDefinition REMOVE_MIME = new SimpleOperationDefinitionBuilder("remove-mime", WebExtension.getResourceDescriptionResolver("container.mime-mapping"))
               .addParameter(MIME_NAME)
               .build();

    private WebContainerDefinition() {
        super(WebExtension.CONTAINER_PATH,
                WebExtension.getResourceDescriptionResolver(Constants.CONTAINER),
                WebContainerAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }


    /**
     * {@inheritDoc}
     * Registers an add operation handler or a remove operation handler if one was provided to the constructor.
     */
    @Override
    public void registerOperations(ManagementResourceRegistration container) {
        super.registerOperations(container);
        container.registerOperationHandler(ADD_MIME,MimeMappingAdd.INSTANCE);
        container.registerOperationHandler(REMOVE_MIME,MimeMappingRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration container) {
        container.registerReadWriteAttribute(WELCOME_FILES, null, new ReloadRequiredWriteAttributeHandler(WELCOME_FILES));
        container.registerReadWriteAttribute(MIME_MAPPINGS, null, new ReloadRequiredWriteAttributeHandler(MIME_MAPPINGS));
    }

}
