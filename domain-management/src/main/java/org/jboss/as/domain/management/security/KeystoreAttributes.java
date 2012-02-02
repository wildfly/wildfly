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

package org.jboss.as.domain.management.security;

import org.jboss.as.domain.management.ModelDescriptionConstants;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;

/**
 * Utility class to hold some {@link AttributeDefinition}s used in different resources.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class KeystoreAttributes {

    public static final SimpleAttributeDefinition KEYSTORE_PASSWORD = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.KEYSTORE_PASSWORD, ModelType.STRING, false)
            .setXmlName(ModelDescriptionConstants.PASSWORD).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition KEYSTORE_PATH = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.KEYSTORE_PATH, ModelType.STRING, false)
            .setXmlName(ModelDescriptionConstants.PATH).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    public static final SimpleAttributeDefinition KEYSTORE_RELATIVE_TO = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.KEYSTORE_RELATIVE_TO, ModelType.STRING, true)
            .setXmlName(ModelDescriptionConstants.RELATIVE_TO).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();

    /** Prevent instantiation */
    private KeystoreAttributes() {}
}
