/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.domain.management.security.LdapAuthorizationResourceDefinition.LdapAuthorizationChildAddHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for group searches where the group references the principal.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class GroupToPrincipalResourceDefinition extends BaseLdapGroupSearchResource {

    public static final SimpleAttributeDefinition BASE_DN = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.BASE_DN, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition RECURSIVE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RECURSIVE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SEARCH_BY = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SEARCH_BY, ModelType.STRING, true)
            .setValidator(new EnumValidator<GroupName>(GroupName.class, true, true, GroupName.DISTINGUISHED_NAME, GroupName.SIMPLE))
            .setDefaultValue(new ModelNode(GroupName.DISTINGUISHED_NAME.toString()))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PRINCIPAL_ATTRIBUTE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PRINCIPAL_ATTRIBUTE, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("member"))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    private static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = {GROUP_NAME, ITERATIVE, GROUP_DN_ATTRIBUTE, GROUP_NAME_ATTRIBUTE, SEARCH_BY, BASE_DN, RECURSIVE, PRINCIPAL_ATTRIBUTE};

    public static final ResourceDefinition INSTANCE = new GroupToPrincipalResourceDefinition();

    private GroupToPrincipalResourceDefinition() {
        super(GroupSearchType.GROUP_TO_PRINCIPAL,
                ControllerResolver.getResolver("core.management.security-realm.authorization.ldap.group-search.group-to-principal"),
                new LdapAuthorizationChildAddHandler(false, ATTRIBUTE_DEFINITIONS), LdapAuthorizationResourceDefinition.REMOVE_INSTANCE);
    }

    @Override
    public AttributeDefinition[] getAttributeDefinitions() {
        return ATTRIBUTE_DEFINITIONS;
    }

}
