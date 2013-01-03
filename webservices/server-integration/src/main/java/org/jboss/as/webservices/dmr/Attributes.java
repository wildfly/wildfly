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
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
interface Attributes {
    SimpleAttributeDefinition WSDL_HOST = new SimpleAttributeDefinitionBuilder(Constants.WSDL_HOST, ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();

    SimpleAttributeDefinition WSDL_PORT = new SimpleAttributeDefinitionBuilder(Constants.WSDL_PORT, ModelType.INT)
            .setAllowNull(true)
            .setMinSize(1)
            .setValidator(new IntRangeValidator(1, true, true))
            .build();

    SimpleAttributeDefinition WSDL_SECURE_PORT = new SimpleAttributeDefinitionBuilder(Constants.WSDL_SECURE_PORT, ModelType.INT)
            .setAllowNull(true)
            .setMinSize(1)
            .setValidator(new IntRangeValidator(1, true, true))
            .build();

    SimpleAttributeDefinition MODIFY_WSDL_ADDRESS = new SimpleAttributeDefinitionBuilder(Constants.MODIFY_WSDL_ADDRESS, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(true))
            .setMinSize(1)
            .build();
    SimpleAttributeDefinition[] SUBSYSTEM_ATTRIBUTES = {MODIFY_WSDL_ADDRESS, WSDL_HOST, WSDL_PORT, WSDL_SECURE_PORT};

    SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(Constants.VALUE, ModelType.STRING)
            .setAllowNull(true)
            .build();

    SimpleAttributeDefinition CLASS = new SimpleAttributeDefinitionBuilder(Constants.CLASS, ModelType.STRING)
            .setAllowNull(false)
            .build();

    SimpleAttributeDefinition PROTOCOL_BINDINGS = new SimpleAttributeDefinitionBuilder(Constants.PROTOCOL_BINDINGS, ModelType.STRING)
            .setAllowNull(true)
            .build();


}
