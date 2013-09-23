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
package org.jboss.as.controller.operations.global;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class GlobalOperationAttributes {

    static final SimpleAttributeDefinition RECURSIVE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RECURSIVE, ModelType.BOOLEAN)
    .setAllowNull(true)
    .setDefaultValue(new ModelNode(false))
    .build();

    static final SimpleAttributeDefinition RECURSIVE_DEPTH = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RECURSIVE_DEPTH, ModelType.INT)
    .setAllowNull(true)
    .setDefaultValue(new ModelNode(0))
    .build();

    static final SimpleAttributeDefinition PROXIES = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PROXIES, ModelType.BOOLEAN)
    .setAllowNull(true)
    .setDefaultValue(new ModelNode(false))
    .build();

    static final SimpleAttributeDefinition INCLUDE_RUNTIME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INCLUDE_RUNTIME, ModelType.BOOLEAN)
    .setAllowNull(true)
    .setDefaultValue(new ModelNode(false))
    .build();

    static final SimpleAttributeDefinition INCLUDE_DEFAULTS = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INCLUDE_DEFAULTS, ModelType.BOOLEAN)
    .setAllowNull(true)
    .setDefaultValue(new ModelNode(true))
    .build();

    static final SimpleAttributeDefinition INCLUDE_ALIASES = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INCLUDE_ALIASES, ModelType.BOOLEAN)
    .setAllowNull(true)
    .setDefaultValue(new ModelNode(false))
    .build();

    static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME, ModelType.STRING)
    .setValidator(new StringLengthValidator(1))
    .setAllowNull(false)
    .build();

    static final SimpleAttributeDefinition LOCALE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.LOCALE, ModelType.STRING)
    .setAllowNull(true)
    .build();

    static final SimpleAttributeDefinition CHILD_TYPE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CHILD_TYPE, ModelType.STRING)
    .setValidator(new StringLengthValidator(1))
    .setAllowNull(false)
    .build();

    static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.VALUE, ModelType.STRING)
    .setValidator(new StringLengthValidator(1))
    .setAllowNull(true)
    .build();

}
