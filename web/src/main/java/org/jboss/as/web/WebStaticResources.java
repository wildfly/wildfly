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

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 24.2.12 12:26
 */
public class WebStaticResources extends SimpleResourceDefinition {
    public static final WebStaticResources INSTANCE = new WebStaticResources();

    protected static final SimpleAttributeDefinition LISTINGS =
            new SimpleAttributeDefinitionBuilder(Constants.LISTINGS, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.LISTINGS)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true))
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition SENDFILE =
            new SimpleAttributeDefinitionBuilder(Constants.SENDFILE, ModelType.INT, true)
                    .setXmlName(Constants.SENDFILE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new IntRangeValidator(1, true))
                    .setDefaultValue(new ModelNode(49152))
                    .build();
    protected static final SimpleAttributeDefinition FILE_ENCODING =
            new SimpleAttributeDefinitionBuilder(Constants.FILE_ENCODING, ModelType.STRING, true)
                    .setXmlName(Constants.FILE_ENCODING)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition READ_ONLY =
            new SimpleAttributeDefinitionBuilder(Constants.READ_ONLY, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.READ_ONLY)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true))
                    .setDefaultValue(new ModelNode(true))
                    .build();
    protected static final SimpleAttributeDefinition WEBDAV =
            new SimpleAttributeDefinitionBuilder(Constants.WEBDAV, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.WEBDAV)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true))
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition SECRET =
            new SimpleAttributeDefinitionBuilder(Constants.SECRET, ModelType.STRING, true)
                    .setXmlName(Constants.SECRET)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();
    protected static final SimpleAttributeDefinition MAX_DEPTH =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_DEPTH, ModelType.INT, true)
                    .setXmlName(Constants.MAX_DEPTH)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new IntRangeValidator(1, true))
                    .setDefaultValue(new ModelNode(3))
                    .build();

    protected static final SimpleAttributeDefinition DISABLED =
            new SimpleAttributeDefinitionBuilder(Constants.DISABLED, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.DISABLED)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModelTypeValidator(ModelType.BOOLEAN, true))
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition[] STATIC_ATTRIBUTES = {
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            LISTINGS,
            SENDFILE,
            FILE_ENCODING,
            READ_ONLY,
            WEBDAV,
            SECRET,
            MAX_DEPTH,
            DISABLED
    };

    private WebStaticResources() {
        super(WebExtension.STATIC_RESOURCES_PATH,
                WebExtension.getResourceDescriptionResolver("configuration.static"),
                WebStaticResourcesAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resources) {
        for (SimpleAttributeDefinition def : STATIC_ATTRIBUTES) {
            resources.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }

    }
}
