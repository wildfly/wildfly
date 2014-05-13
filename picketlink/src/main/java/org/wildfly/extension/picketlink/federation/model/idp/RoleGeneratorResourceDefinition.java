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

package org.wildfly.extension.picketlink.federation.model.idp;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.model.AbstractFederationResourceDefinition;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class RoleGeneratorResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition CLASS_NAME = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_CLASS_NAME.getName(), ModelType.STRING, true)
        .setAllowExpression(true)
        .setAlternatives(ModelElement.COMMON_CODE.getName())
        .build();
    public static final SimpleAttributeDefinition CODE = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_CODE.getName(), ModelType.STRING, true)
        .setValidator(new EnumValidator<RoleGeneratorTypeEnum>(RoleGeneratorTypeEnum.class, true, true))
        .setAllowExpression(true)
        .setAlternatives(ModelElement.COMMON_CLASS_NAME.getName())
        .build();
    public static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_MODULE.getName(), ModelType.STRING, true)
        .setAllowExpression(true)
        .setRequires(ModelElement.COMMON_CLASS_NAME.getName())
        .build();
    public static final SimpleAttributeDefinition[] ATTRIBUTE_DEFINITIONS = new SimpleAttributeDefinition[]{CLASS_NAME, CODE, MODULE};
    public static final RoleGeneratorResourceDefinition INSTANCE = new RoleGeneratorResourceDefinition();

    private RoleGeneratorResourceDefinition() {
        super(ModelElement.IDENTITY_PROVIDER_ROLE_GENERATOR, new IdentityProviderConfigAddStepHandler(ATTRIBUTE_DEFINITIONS), IdentityProviderConfigRemoveStepHandler.INSTANCE, ATTRIBUTE_DEFINITIONS);
    }
}
