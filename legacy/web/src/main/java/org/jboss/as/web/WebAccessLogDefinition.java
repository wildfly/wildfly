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

import java.util.List;

import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 16:33
 */
public class WebAccessLogDefinition extends ModelOnlyResourceDefinition {


    protected static final SimpleAttributeDefinition PATTERN =
            new SimpleAttributeDefinitionBuilder(Constants.PATTERN, ModelType.STRING, true)
                    .setXmlName(Constants.PATTERN)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("common"))
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition RESOLVE_HOSTS =
            new SimpleAttributeDefinitionBuilder(Constants.RESOLVE_HOSTS, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.RESOLVE_HOSTS)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition EXTENDED =
            new SimpleAttributeDefinitionBuilder(Constants.EXTENDED, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.EXTENDED)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition PREFIX =
            new SimpleAttributeDefinitionBuilder(Constants.PREFIX, ModelType.STRING, true)
                    .setXmlName(Constants.PREFIX)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("access_log."))
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition ROTATE =
            new SimpleAttributeDefinitionBuilder(Constants.ROTATE, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.ROTATE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(true))
                    .build();
    protected static final SimpleAttributeDefinition[] ACCESS_LOG_ATTRIBUTES = {
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            PATTERN,
            RESOLVE_HOSTS,
            EXTENDED,
            PREFIX,
            ROTATE
    };

    static final WebAccessLogDefinition INSTANCE = new WebAccessLogDefinition();

    private final List<AccessConstraintDefinition> accessConstraints;


    private WebAccessLogDefinition() {
        super(WebExtension.ACCESS_LOG_PATH,
                WebExtension.getResourceDescriptionResolver("virtual-server.access-log"),
                ACCESS_LOG_ATTRIBUTES);
        SensitivityClassification sc = new SensitivityClassification(WebExtension.SUBSYSTEM_NAME, "web-access-log", false, false, false);
        this.accessConstraints = new SensitiveTargetAccessConstraintDefinition(sc).wrapAsList();
        setDeprecated(WebExtension.DEPRECATED_SINCE);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }
}
