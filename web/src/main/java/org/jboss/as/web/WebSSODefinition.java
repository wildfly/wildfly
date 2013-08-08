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

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.constraint.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.constraint.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Tomaz Cerar
 * @created 23.2.12 16:54
 */
public class WebSSODefinition extends SimpleResourceDefinition {
    public static final WebSSODefinition INSTANCE = new WebSSODefinition();

    protected static final SimpleAttributeDefinition CACHE_CONTAINER =
            new SimpleAttributeDefinitionBuilder(Constants.CACHE_CONTAINER, ModelType.STRING, true)
                    .setValidator(new StringLengthValidator(1, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition CACHE_NAME =
            new SimpleAttributeDefinitionBuilder(Constants.CACHE_NAME, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true))
                    .build();

    protected static final SimpleAttributeDefinition DOMAIN =
            new SimpleAttributeDefinitionBuilder(Constants.DOMAIN, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .build();

    protected static final SimpleAttributeDefinition REAUTHENTICATE =
            new SimpleAttributeDefinitionBuilder(Constants.REAUTHENTICATE, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static SimpleAttributeDefinition[] SSO_ATTRIBUTES = {
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            CACHE_CONTAINER, CACHE_NAME, DOMAIN, REAUTHENTICATE
    };

    private final List<AccessConstraintDefinition> accessConstraints;

    private WebSSODefinition() {
        super(WebExtension.SSO_PATH,
                WebExtension.getResourceDescriptionResolver("virtual-server.sso"),
                WebSSOAdd.INSTANCE,
                new ReloadRequiredRemoveStepHandler());
        SensitivityClassification sc = new SensitivityClassification(WebExtension.SUBSYSTEM_NAME, "web-sso", false, true, true);
        this.accessConstraints = new SensitiveTargetAccessConstraintDefinition(sc).wrapAsList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration sso) {
        for (SimpleAttributeDefinition def : SSO_ATTRIBUTES) {
            sso.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }
}
