/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JASPIMappingModuleDefinition extends MappingModuleDefinition {

    static final SimpleAttributeDefinition LOGIN_MODULE_STACK_REF = new SimpleAttributeDefinitionBuilder(Constants.LOGIN_MODULE_STACK_REF, ModelType.STRING)
            .setRequired(false)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    private static final SimpleAttributeDefinition FLAG = new SimpleAttributeDefinitionBuilder(Constants.FLAG, ModelType.STRING)
            .setRequired(false)
            .setValidator(new EnumValidator<ModuleFlag>(ModuleFlag.class, true, true))
            .setAllowExpression(true)
            .build();



    private static final AttributeDefinition[] ATTRIBUTES = {CODE, FLAG, LOGIN_MODULE_STACK_REF, MODULE_OPTIONS, LoginModuleResourceDefinition.MODULE};

    JASPIMappingModuleDefinition() {
        super(Constants.AUTH_MODULE);
    }

    @Override
    public AttributeDefinition[] getAttributes() {
        return ATTRIBUTES;
    }
}
