/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2012, Red Hat Middleware LLC, and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.webservices.dmr;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
interface Attributes {
    SimpleAttributeDefinition WSDL_HOST = new SimpleAttributeDefinitionBuilder(Constants.WSDL_HOST, ModelType.STRING)
            .setRequired(false)
            .setMinSize(1)
            .setAllowExpression(true)
            .setValidator(new AddressValidator(true, true))
            .build();

    SimpleAttributeDefinition WSDL_PORT = new SimpleAttributeDefinitionBuilder(Constants.WSDL_PORT, ModelType.INT)
            .setRequired(false)
            .setMinSize(1)
            .setValidator(new IntRangeValidator(1, true, true))
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition WSDL_SECURE_PORT = new SimpleAttributeDefinitionBuilder(Constants.WSDL_SECURE_PORT, ModelType.INT)
            .setRequired(false)
            .setMinSize(1)
            .setValidator(new IntRangeValidator(1, true, true))
            .setAllowExpression(true)
            .build();
    SimpleAttributeDefinition WSDL_URI_SCHEME = new SimpleAttributeDefinitionBuilder(Constants.WSDL_URI_SCHEME, ModelType.STRING)
            .setRequired(false)
            .setMinSize(1)
            .setValidator(new EnumValidator<>(WsdlUriSchema.class, false, false))
            .setAllowExpression(true)
            .build();
    enum WsdlUriSchema {http, https}

    SimpleAttributeDefinition MODIFY_WSDL_ADDRESS = new SimpleAttributeDefinitionBuilder(Constants.MODIFY_WSDL_ADDRESS, ModelType.BOOLEAN)
            .setRequired(false)
            .setDefaultValue(new ModelNode(true))
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition WSDL_PATH_REWRITE_RULE = new SimpleAttributeDefinitionBuilder(Constants.WSDL_PATH_REWRITE_RULE, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(false)
            .build();

    SimpleAttributeDefinition STATISTICS_ENABLED = new SimpleAttributeDefinitionBuilder(Constants.STATISTICS_ENABLED, ModelType.BOOLEAN)
            .setRequired(false)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition[] SUBSYSTEM_ATTRIBUTES = {MODIFY_WSDL_ADDRESS, WSDL_HOST, WSDL_PORT, WSDL_SECURE_PORT, WSDL_URI_SCHEME, WSDL_PATH_REWRITE_RULE};

    SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(Constants.VALUE, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    SimpleAttributeDefinition CLASS = new SimpleAttributeDefinitionBuilder(Constants.CLASS, ModelType.STRING)
            .setRequired(true)
            .build();

    SimpleAttributeDefinition PROTOCOL_BINDINGS = new SimpleAttributeDefinitionBuilder(Constants.PROTOCOL_BINDINGS, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .build();
}
